package com.rice.rice.service;

import com.rice.rice.common.BusinessException;
import com.rice.rice.dto.ChatRequest;
import com.rice.rice.dto.DiagnosisRequest;
import com.rice.rice.dto.DiseasePredictionResult;
import com.rice.rice.dto.PageQuery;
import com.rice.rice.dto.PagedData;
import com.rice.rice.entity.ChatMessage;
import com.rice.rice.entity.ChatSession;
import com.rice.rice.entity.DetectionRecord;
import com.rice.rice.mapper.ChatMessageMapper;
import com.rice.rice.mapper.ChatSessionMapper;
import com.rice.rice.mapper.DetectionRecordMapper;
import com.rice.rice.util.SecurityUtils;
import com.rice.rice.util.TimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AiService {

    // ==================== 依赖注入 ====================
    private final DetectionRecordMapper detectionRecordMapper;      // 检测记录数据访问层
    private final ChatSessionMapper chatSessionMapper;              // 聊天会话数据访问层
    private final ChatMessageMapper chatMessageMapper;              // 聊天消息数据访问层
    private final OpenAiChatClient openAiChatClient;                // 大语言模型客户端（通义千问）
    private final com.rice.rice.config.LlmProperties llmProperties; // LLM 配置属性
    private final RiceDiseasePredictor riceDiseasePredictor;        // Python 病害识别模型服务
    private final boolean pythonApiEnabled;                         // 是否启用 Python API（配置文件控制）

    public AiService(
            DetectionRecordMapper detectionRecordMapper,
            ChatSessionMapper chatSessionMapper,
            ChatMessageMapper chatMessageMapper,
            OpenAiChatClient openAiChatClient,
            com.rice.rice.config.LlmProperties llmProperties,
            RiceDiseasePredictor riceDiseasePredictor,
            @Value("${app.python-api.enabled:true}") boolean pythonApiEnabled) {
        this.detectionRecordMapper = detectionRecordMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.openAiChatClient = openAiChatClient;
        this.llmProperties = llmProperties;
        this.riceDiseasePredictor = riceDiseasePredictor;
        this.pythonApiEnabled = pythonApiEnabled;
    }

    /**
     * 水稻病虫害诊断（核心业务逻辑）
     * 流程：接收图片 → 调用 Python 模型识别 → 保存检测记录 → 创建聊天会话
     *
     * @param req 诊断请求（包含图片 URL、品种、症状描述）
     * @return 诊断结果（包含分析详情、memoryId 等）
     */
    @Transactional
    public Map<String, Object> diagnosis(DiagnosisRequest req) {
        // 1. 获取当前登录用户 ID（从 JWT Token 中解析）
        Long userId = SecurityUtils.requireUserId();
        
        // 2. 生成唯一的会话标识符（用于关联检测记录和聊天记录）
        String memoryId = UUID.randomUUID().toString().replace("-", "");
        String variety = req.getRiceVariety() != null ? req.getRiceVariety() : "";
        String symptom = req.getSymptomDesc() != null ? req.getSymptomDesc() : "";
        
        String analysis;   // 详细分析报告（Markdown 格式）
        String shortDiag;  // 简短诊断摘要（用于列表展示）
        
        // 3. 根据配置决定使用真实模型还是模拟数据
        if (pythonApiEnabled) {
            // 【真实模式】调用 Python Flask 服务的 /predict_base64 接口
            DiseasePredictionResult pred = riceDiseasePredictor.predictFromImageUrl(req.getImageUrl());
            analysis = formatModelAnalysisMarkdown(pred, variety, symptom);
            shortDiag = buildShortDiagnosisSummary(pred);
        } else {
            // 【模拟模式】生成占位文本，用于前端联调演示
            analysis = buildMockAnalysis(variety, symptom);
            shortDiag = analysis.length() > 120 ? analysis.substring(0, 120) + "…" : analysis;
        }

        // 4. 保存检测记录到数据库
        DetectionRecord dr = new DetectionRecord();
        dr.setUserId(userId);
        dr.setRiceVariety(variety);
        dr.setSymptomDesc(symptom);
        dr.setImageUrl(req.getImageUrl());
        dr.setAnalysisResult(analysis);       // 完整分析报告
        dr.setMemoryId(memoryId);             // 会话标识（关联聊天记录）
        dr.setDiagnosis(shortDiag.replace("\n", " "));  // 简短摘要（去除换行）
        dr.setCreateTime(LocalDateTime.now());
        detectionRecordMapper.save(dr);

        // 5. 创建聊天会话（方便用户后续追问）
        ChatSession session = new ChatSession();
        session.setMemoryId(memoryId);
        session.setUserId(userId);
        session.setFirstQuestion(truncate(symptom.isEmpty() ? "水稻病虫害识别" : symptom, 200));
        session.setCreateTime(LocalDateTime.now());
        session.setExpirationTime(LocalDateTime.now().plusDays(30));  // 30 天后过期
        chatSessionMapper.save(session);

        // 6. 返回检测结果
        Map<String, Object> data = toDetectionMap(dr);
        return data;
    }

    /**
     * 普通对话（非流式）
     * 流程：验证权限 → 加载历史上下文 → 调用 LLM → 保存消息记录
     *
     * @param req 聊天请求（包含问题、memoryId）
     * @return AI 回复内容（失败时返回错误提示）
     */
    public String chat(ChatRequest req) {
        // 1. 获取当前用户并验证会话权限
        Long userId = SecurityUtils.requireUserId();
        // 如果 memoryId 为空或空白，自动生成一个新的
        String memoryId = (req.getMemoryId() != null && !req.getMemoryId().trim().isEmpty()) 
                ? req.getMemoryId() 
                : UUID.randomUUID().toString().replace("-", "");
        
        // 检查会话是否存在且属于当前用户
        chatSessionMapper.findByMemoryId(memoryId).ifPresent(s -> {
            if (!s.getUserId().equals(userId)) {
                throw new BusinessException("无权访问该会话");
            }
        });
        
        // 查找或创建会话
        ChatSession session = chatSessionMapper
                .findByMemoryIdAndUserId(memoryId, userId)
                .orElseGet(() -> createSession(userId, memoryId, req.getQuestion()));

        // 2. 保存用户发送的消息到数据库
        ChatMessage userMsg = new ChatMessage();
        userMsg.setMemoryId(session.getMemoryId());
        userMsg.setRole("user");
        userMsg.setContent(req.getQuestion());
        userMsg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.save(userMsg);

        // 3. 尝试调用 AI 服务，如果失败则保存错误消息（优雅降级）
        String reply;
        try {
            // 构建对话上下文（system prompt + 最近的历史消息）
            List<Map<String, String>> messages = buildChatContext(session.getMemoryId());
            reply = openAiChatClient.chat(llmProperties.getModel(), messages);
        } catch (Exception e) {
            // AI 调用失败，保存错误回复到数据库
            String errorMsg = "AI 服务暂时不可用（" + e.getClass().getSimpleName() + "），请稍后重试。\n\n错误详情：" + e.getMessage();
            
            ChatMessage bot = new ChatMessage();
            bot.setMemoryId(session.getMemoryId());
            bot.setRole("assistant");
            bot.setContent(errorMsg);
            bot.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.save(bot);
            
            // 直接返回错误信息，不抛异常，避免数据回滚
            return errorMsg;
        }

        // 4. 保存 AI 回复到数据库
        ChatMessage bot = new ChatMessage();
        bot.setMemoryId(session.getMemoryId());
        bot.setRole("assistant");
        bot.setContent(reply);
        bot.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.save(bot);

        return reply;
    }

    /**
     * 流式对话：写入用户消息后按 token 回调，结束后写入 assistant 整段回复。
     * 不加类级别事务，避免长连接占用数据库事务。
     */
    public String chatStream(ChatRequest req, Consumer<String> onChunk) {
        Long userId = SecurityUtils.requireUserId();
        String memoryId = (req.getMemoryId() != null && !req.getMemoryId().trim().isEmpty())
                ? req.getMemoryId().trim()
                : UUID.randomUUID().toString().replace("-", "");

        chatSessionMapper.findByMemoryId(memoryId).ifPresent(s -> {
            if (!s.getUserId().equals(userId)) {
                throw new BusinessException("无权访问该会话");
            }
        });
        ChatSession session = chatSessionMapper
                .findByMemoryIdAndUserId(memoryId, userId)
                .orElseGet(() -> createSession(userId, memoryId, req.getQuestion()));

        ChatMessage userMsg = new ChatMessage();
        userMsg.setMemoryId(session.getMemoryId());
        userMsg.setRole("user");
        userMsg.setContent(req.getQuestion());
        userMsg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.save(userMsg);

        List<Map<String, String>> messages = buildChatContext(session.getMemoryId());
        String reply = openAiChatClient.chatStream(llmProperties.getModel(), messages, onChunk);

        ChatMessage bot = new ChatMessage();
        bot.setMemoryId(session.getMemoryId());
        bot.setRole("assistant");
        bot.setContent(reply != null ? reply : "");
        bot.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.save(bot);

        return reply != null ? reply : "";
    }

    private List<Map<String, String>> buildChatContext(String memoryId) {
        List<Map<String, String>> out = new ArrayList<>();
        out.add(Map.of("role", "system", "content", llmProperties.getSystemPrompt()));

        int max = llmProperties.getMaxContextMessages();
        // 这里简单取最近 20 条（ChatMessageMapper 固定 top20）；若你调大 max，可再扩展 Mapper 方法。
        List<ChatMessage> recent = chatMessageMapper.findTop20ByMemoryIdOrderByCreatedAtDesc(memoryId);
        Collections.reverse(recent); // 从旧到新
        for (ChatMessage m : recent) {
            String role = m.getRole();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            out.add(Map.of("role", role, "content", m.getContent()));
        }

        if (out.size() > 1 + max) {
            // 保留 systemPrompt + 最近 max 条
            return new ArrayList<>(out.subList(out.size() - max, out.size()));
        }
        return out;
    }

    private ChatSession createSession(Long userId, String memoryId, String firstQuestion) {
        ChatSession s = new ChatSession();
        s.setMemoryId(memoryId);
        s.setUserId(userId);
        s.setFirstQuestion(truncate(firstQuestion, 200));
        s.setCreateTime(LocalDateTime.now());
        s.setExpirationTime(LocalDateTime.now().plusDays(30));
        return chatSessionMapper.save(s);
    }

    public PagedData<Map<String, Object>> detectionPage(PageQuery q) {
        Long userId = SecurityUtils.requireUserId();
        Page<DetectionRecord> page = detectionRecordMapper.findByUserIdOrderByCreateTimeDesc(
                userId,
                PageRequest.of(q.getPage() - 1, q.getPageSize()));
        List<Map<String, Object>> list = new ArrayList<>();
        for (DetectionRecord dr : page.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("detectionRecordId", dr.getDetectionRecordId());
            row.put("diagnosis", dr.getDiagnosis());
            row.put("createTime", TimeFormat.iso(dr.getCreateTime()));
            row.put("memoryId", dr.getMemoryId());
            row.put("imageUrl", dr.getImageUrl());
            list.add(row);
        }
        return new PagedData<>(list, page.getTotalElements());
    }

    public Map<String, Object> detectionDetail(Long id) {
        Long userId = SecurityUtils.requireUserId();
        DetectionRecord dr = detectionRecordMapper.findById(id).orElseThrow(() -> new BusinessException("记录不存在"));
        if (!userId.equals(dr.getUserId())) {
            throw new BusinessException("无权查看该记录");
        }
        return toDetectionMap(dr);
    }

    /**
     * 分页查询聊天会话列表
     *
     * @param q 分页参数（page、pageSize）
     * @return 分页数据（包含会话摘要信息）
     */
    public PagedData<Map<String, Object>> chatPage(PageQuery q) {
        Long userId = SecurityUtils.requireUserId();
        // 查询当前用户的聊天会话（按时间倒序）
        Page<ChatSession> page = chatSessionMapper.findByUserIdOrderByCreateTimeDesc(
                userId,
                PageRequest.of(q.getPage() - 1, q.getPageSize()));
        
        // 转换为前端需要的格式
        List<Map<String, Object>> list = new ArrayList<>();
        for (ChatSession s : page.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("chatRecodeId", s.getChatRecodeId());
            row.put("memoryId", s.getMemoryId());
            row.put("firstQuestion", s.getFirstQuestion());  // 第一个问题作为标题
            row.put("createTime", TimeFormat.iso(s.getCreateTime()));
            row.put("expirationTime", TimeFormat.iso(s.getExpirationTime()));
            row.put("userId", s.getUserId());
            list.add(row);
        }
        return new PagedData<>(list, page.getTotalElements());
    }

    /**
     * 查询聊天会话详情（包含所有消息记录）
     *
     * @param memoryId 会话标识
     * @return 包含消息列表的 Map（messages 字段）
     */
    public Map<String, Object> chatDetail(String memoryId) {
        Long userId = SecurityUtils.requireUserId();
        // 验证会话是否存在且属于当前用户
        ChatSession s = chatSessionMapper
                .findByMemoryIdAndUserId(memoryId, userId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        
        // 获取该会话下的所有消息（按时间升序，从旧到新）
        List<ChatMessage> msgs = chatMessageMapper.findByMemoryIdOrderByCreatedAtAsc(s.getMemoryId());
        
        // 转换为前端需要的格式
        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage m : msgs) {
            Map<String, String> one = new LinkedHashMap<>();
            one.put("role", m.getRole());      // user 或 assistant
            one.put("content", m.getContent()); // 消息内容
            messages.add(one);
        }
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("messages", messages);
        return data;
    }

    /**
     * 删除一条病虫害检测记录（仅本人数据）
     * 注意：只删除检测记录，不删除关联的聊天会话和消息
     *
     * @param detectionRecordId 检测记录 ID
     */
    @Transactional
    public void deleteDetectionRecord(Long detectionRecordId) {
        Long userId = SecurityUtils.requireUserId();
        // 查找记录并验证权限
        DetectionRecord dr = detectionRecordMapper
                .findById(detectionRecordId)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        if (!userId.equals(dr.getUserId())) {
            throw new BusinessException("无权删除该记录");
        }
        // 执行删除
        detectionRecordMapper.delete(dr);
    }

    /**
     * 删除聊天会话及其所有消息记录
     * 级联删除：先删除消息，再删除会话
     *
     * @param memoryId 会话标识
     */
    @Transactional
    public void deleteChat(String memoryId) {
        Long userId = SecurityUtils.requireUserId();
        
        // 1. 验证会话是否存在且属于当前用户
        ChatSession session = chatSessionMapper
                .findByMemoryIdAndUserId(memoryId, userId)
                .orElseThrow(() -> new BusinessException("会话不存在或无权删除"));
        
        // 2. 先删除该会话下的所有消息（避免外键约束）
        chatMessageMapper.deleteByMemoryId(memoryId);
        
        // 3. 再删除会话本身
        chatSessionMapper.deleteByMemoryId(memoryId);
    }

    private Map<String, Object> toDetectionMap(DetectionRecord dr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("imgUrl", dr.getImageUrl());
        m.put("analysisResult", dr.getAnalysisResult());
        m.put("createTime", TimeFormat.iso(dr.getCreateTime()));
        m.put("detectionRecordId", dr.getDetectionRecordId());
        m.put("memoryId", dr.getMemoryId());
        return m;
    }

    /**
     * 字符串截断工具方法（防止过长文本）
     *
     * @param s   原始字符串
     * @param max 最大长度
     * @return 截断后的字符串（超长时添加省略号）
     */
    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * 将 Python 模型的预测结果格式化为 Markdown 报告
     * 包含：预测类别、置信度、品种、症状、防治建议、概率分布
     *
     * @param p       预测结果对象
     * @param variety 水稻品种
     * @param symptom 症状描述
     * @return Markdown 格式的完整报告
     */
    private static String formatModelAnalysisMarkdown(DiseasePredictionResult p, String variety, String symptom) {
        StringBuilder sb = new StringBuilder();
        sb.append("水稻病虫害识别结果（深度学习模型）\n\n");
        
        // 预测类别（中英文）
        sb.append("预测类别：").append(nullToEmpty(p.getClassCn()))
                .append("（`").append(nullToEmpty(p.getClassEn())).append("`）\n\n");
        
        // 置信度
        if (p.getConfidence() != null) {
            sb.append("置信度：").append(String.format("%.2f%%", p.getConfidence() * 100)).append("\n\n");
        }
        
        // 品种信息
        if (!variety.isEmpty()) {
            sb.append("品种：").append(variety).append("\n\n");
        }
        
        // 症状描述
        if (!symptom.isEmpty()) {
            sb.append("症状描述：").append(symptom).append("\n\n");
        }
        
        // 防治建议
        if (p.getTreatment() != null && !p.getTreatment().isBlank()) {
            sb.append("防治建议\n").append(p.getTreatment()).append("\n\n");
        }
        
        // Top 5 概率分布
        if (p.getAllProbs() != null && !p.getAllProbs().isEmpty()) {
            sb.append("各类别概率（Top 5）\n");
            p.getAllProbs().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())  // 按概率降序
                    .limit(5)  // 只取前 5 个
                    .forEach(e -> {
                        String className = e.getKey();
                        // 尝试获取中文名称（如果 key 是英文类别名）
                        String displayName = getChineseClassName(className);
                        sb.append("- **")
                                .append(displayName)
                                .append("**：")
                                .append(String.format("%.2f%%", e.getValue() * 100))
                                .append("\n");
                    });
        }
        return sb.toString();
    }

    /**
     * 构建简短的诊断摘要（用于列表展示）
     * 格式：病害名称 + 置信度百分比
     *
     * @param p 预测结果对象
     * @return 简短摘要字符串
     */
    private static String buildShortDiagnosisSummary(DiseasePredictionResult p) {
        String cn = nullToEmpty(p.getClassCn());
        if (p.getConfidence() != null) {
            return (cn + " " + String.format("%.0f%%", p.getConfidence() * 100)).trim();
        }
        return cn.isEmpty() ? "识别完成" : cn;
    }

    /**
     * 空值安全转换：null 转空字符串
     *
     * @param s 原始字符串
     * @return 非空字符串（null 时返回 ""）
     */
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 将英文类别名转换为中文名称
     * 如果找不到对应的中文名称，则返回原始英文名称
     *
     * @param classEn 英文类别名
     * @return 中文名称或原始英文名
     */
    private static String getChineseClassName(String classEn) {
        if (classEn == null || classEn.isEmpty()) {
            return classEn;
        }
        
        // 水稻病害类别中英文对照表
        switch (classEn.toLowerCase()) {
            case "bacterial_leaf_blight":
                return "白叶枯病";
            case "brown_spot":
                return "褐斑病";
            case "healthy":
                return "健康植株";
            case "leaf_blast":
                return "叶瘟病";
            case "leaf_scald":
                return "叶烧病";
            case "narrow_brown_spot":
                return "窄褐条斑病";
            case "neck_blast":
                return "颈瘟病";
            case "rice_hispa":
                return "水稻铁甲虫";
            case "sheath_blight":
                return "纹枯病";
            case "tungro":
                return "东格鲁病";
            default:
                // 如果没有匹配，返回原始名称
                return classEn;
        }
    }

    /**
     * 生成模拟的分析报告（用于无 Python 环境时的联调演示）
     * 包含：品种、症状、初步判断、建议措施
     *
     * @param variety 水稻品种
     * @param symptom 症状描述
     * @return Markdown 格式的模拟报告
     */
    private static String buildMockAnalysis(String variety, String symptom) {
        return "水稻病害分析报告\n\n"
                + "品种："
                + (variety.isEmpty() ? "未填写" : variety)
                + "\n\n症状描述："
                + (symptom.isEmpty() ? "无" : symptom)
                + "\n\n初步判断\n"
                + "本结果为模拟分析（后端未接入真实识别模型），用于联调与演示。请结合田间观察与农技指导综合判断。\n\n"
                + "建议措施\n"
                + "1. 保持田间通风透光，合理灌溉。\n"
                + "2. 发现异常及时取样送检或咨询当地植保站。\n";
    }

    /**
     * 生成模拟的聊天回复（用于无 LLM 环境时的联调演示）
     *
     * @param question 用户问题
     * @return 模拟回复文本
     * @deprecated 已不再使用，保留用于兼容
     */
    private static String buildMockChatReply(String question) {
        return "（模拟回复）已收到您的问题：「"
                + truncate(question, 80)
                + "」。当前后端未接入大模型，此为占位回答，便于前端展示 Markdown 与多轮会话流程。";
    }
}
