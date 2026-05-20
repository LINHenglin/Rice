package com.rice.rice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rice.rice.common.ApiResult;
import com.rice.rice.dto.ChatRequest;
import com.rice.rice.dto.DetectionRecordDeleteRequest;
import com.rice.rice.dto.DiagnosisRequest;
import com.rice.rice.dto.PageQuery;
import com.rice.rice.dto.PagedData;
import com.rice.rice.service.AiService;
import com.rice.rice.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/user/ai")  // 所有 AI 相关接口的统一前缀
public class AiController {

    // ==================== 依赖注入 ====================
    private final AiService aiService;                    // AI 业务逻辑层
    private final FileStorageService fileStorageService;  // 文件存储服务（图片上传）
    private final ObjectMapper objectMapper;              // JSON 序列化工具（用于 SSE 流式响应）

    public AiController(AiService aiService, FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * 病害识别接口（JSON 方式）
     * 前端通过 POST 请求发送包含 imageUrl 的 JSON 数据
     *
     * @param req 诊断请求（包含图片 URL、品种、症状描述）
     * @return 识别结果（包含分析报告、memoryId 等）
     */
    @PostMapping(value = "/diagnosis", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Map<String, Object>> diagnosisJson(@Valid @RequestBody DiagnosisRequest req) {
        return ApiResult.ok(aiService.diagnosis(req));
    }

    /**
     * 病害识别接口（表单上传方式）
     * 前端通过 multipart/form-data 一次性提交图片文件和文本信息
     * 支持两种图片字段名：file 或 image
     *
     * @param file       图片文件（可选）
     * @param image      图片文件（可选，兼容不同前端命名）
     * @param riceVariety 水稻品种
     * @param symptomDesc 症状描述
     * @param imageUrl   图片 URL（如果已提供则不再要求文件）
     * @return 识别结果
     */
    @PostMapping(value = "/diagnosis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<Map<String, Object>> diagnosisMultipart(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "riceVariety", required = false) String riceVariety,
            @RequestParam(value = "symptomDesc", required = false) String symptomDesc,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        // 优先使用已有的 imageUrl
        String url = imageUrl != null ? imageUrl.trim() : "";
        if (url.isEmpty()) {
            // 如果没有 imageUrl，则从上传的文件中获取
            MultipartFile part = multipartFirstNonEmpty(file, image);
            if (part == null) {
                throw new com.rice.rice.common.BusinessException("请上传图片或提供 imageUrl");
            }
            // 将上传的文件保存到服务器并生成 URL
            url = fileStorageService.storeImage(part);
        }
        
        // 构建诊断请求对象
        DiagnosisRequest req = new DiagnosisRequest();
        req.setRiceVariety(riceVariety);
        req.setSymptomDesc(symptomDesc);
        req.setImageUrl(url);
        return ApiResult.ok(aiService.diagnosis(req));
    }

    /**
     * 辅助方法：从两个 MultipartFile 中获取第一个非空的文件
     *
     * @param a 文件 a
     * @param b 文件 b
     * @return 第一个非空的文件，都为空则返回 null
     */
    private static MultipartFile multipartFirstNonEmpty(MultipartFile a, MultipartFile b) {
        if (a != null && !a.isEmpty()) {
            return a;
        }
        if (b != null && !b.isEmpty()) {
            return b;
        }
        return null;
    }

    /**
     * 分页查询检测记录列表
     *
     * @param q 分页参数
     * @return 分页数据
     */
    @PostMapping("/detectionRecodePageQuery")
    public ApiResult<PagedData<Map<String, Object>>> detectionPage(@Valid @RequestBody PageQuery q) {
        return ApiResult.ok(aiService.detectionPage(q));
    }

    /**
     * 查询检测记录详情
     *
     * @param id 检测记录 ID
     * @return 完整的检测信息
     */
    @GetMapping("/detectionRecodeDetail/{id}")
    public ApiResult<Map<String, Object>> detectionDetail(@PathVariable("id") Long id) {
        return ApiResult.ok(aiService.detectionDetail(id));
    }

    /**
     * 删除检测记录（前端路径：/user/ai/detectionRecodeDelete）。
     */
    @PostMapping("/detectionRecodeDelete")
    public ApiResult<Void> deleteDetectionRecord(@Valid @RequestBody DetectionRecordDeleteRequest req) {
        aiService.deleteDetectionRecord(req.getDetectionRecordId());
        return ApiResult.ok(null);
    }

    /**
     * 普通对话接口（非流式）
     * 一次性返回完整的 AI 回复
     *
     * @param req 聊天请求
     * @return AI 回复内容和 memoryId
     */
    @PostMapping("/chat")
    public ApiResult<Map<String, Object>> chat(@Valid @RequestBody ChatRequest req) {
        String reply = aiService.chat(req);
        
        // 返回 AI 回复和 memoryId，方便前端刷新
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("content", reply);  // 前端期望的字段名
        result.put("reply", reply);     // 兼容旧版本
        result.put("memoryId", req.getMemoryId());
        return ApiResult.ok(result);
    }

    /**
     * SSE 流式对话接口
     * 使用 Server-Sent Events 技术实现逐字输出效果
     * 响应格式：每行 data: {"content":"..."}，结束时 data: [DONE]
     *
     * @param req      聊天请求
     * @param response HTTP 响应对象（用于直接写入 SSE 数据）
     */
    @PostMapping("/chat/stream")
    public void chatStream(@Valid @RequestBody ChatRequest req, HttpServletResponse response) throws IOException {
        // 设置 SSE 响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");      // 禁用缓存
        response.setHeader("Connection", "keep-alive");        // 保持连接
        response.setHeader("X-Accel-Buffering", "no");         // 禁用 Nginx 缓冲

        PrintWriter writer = response.getWriter();
        try {
            // 调用流式对话服务，每个 token 回调一次
            aiService.chatStream(req, chunk -> {
                try {
                    // 将每个 token 包装成 JSON 并发送
                    String json = objectMapper.writeValueAsString(Map.of("content", chunk));
                    writer.write("data: " + json + "\n\n");
                    writer.flush();  // 立即发送到客户端
                } catch (Exception ignored) {
                    // 客户端断开时写入可能失败，忽略异常
                }
            });
        } catch (Exception e) {
            // 发生错误时发送错误信息
            try {
                String errJson = objectMapper.writeValueAsString(Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown"));
                writer.write("data: " + errJson + "\n\n");
                writer.flush();
            } catch (Exception ignored) {
            }
        }
        // 发送结束标记
        writer.write("data: [DONE]\n\n");
        writer.flush();
    }

    /**
     * 分页查询聊天会话列表
     *
     * @param q 分页参数
     * @return 分页数据
     */
    @PostMapping("/chatRecodePageQuery")
    public ApiResult<PagedData<Map<String, Object>>> chatPage(@Valid @RequestBody PageQuery q) {
        return ApiResult.ok(aiService.chatPage(q));
    }

    /**
     * 查询聊天会话详情（包含所有消息）
     *
     * @param memoryId 会话标识
     * @return 消息列表
     */
    @GetMapping("/chatRecodeDetail/{memoryId}")
    public ApiResult<Map<String, Object>> chatDetail(@PathVariable("memoryId") String memoryId) {
        return ApiResult.ok(aiService.chatDetail(memoryId));
    }

    /**
     * 删除聊天会话及其所有消息
     *
     * @param request 包含 memoryId 的请求体
     * @return 删除成功提示
     */
    @PostMapping("/chatRecodeDelete")
    public ApiResult<String> deleteChat(@RequestBody Map<String, String> request) {
        String memoryId = request.get("memoryId");
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new com.rice.rice.common.BusinessException("memoryId 不能为空");
        }
        aiService.deleteChat(memoryId);
        return ApiResult.ok("删除成功", "");
    }
}
