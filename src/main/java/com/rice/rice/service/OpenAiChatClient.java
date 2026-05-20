package com.rice.rice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rice.rice.common.BusinessException;
import com.rice.rice.config.LlmProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 通义千问（OpenAI 兼容接口）聊天客户端
 * 基于 OpenAI Chat Completions 兼容接口的最小客户端实现
 * 
 * 功能：
 * 1. 普通对话（chat）- 一次性返回完整响应
 * 2. 流式对话（chatStream）- SSE 逐字输出，实现打字机效果
 * 
 * 注意：不在代码中硬编码 Key，运行时通过配置/环境变量注入
 */
@Service
public class OpenAiChatClient {

    private final LlmProperties props;          // LLM 配置属性
    private final RestClient restClient;        // REST 客户端（用于普通对话）
    private final ObjectMapper objectMapper;    // JSON 序列化器
    private final HttpClient streamHttpClient;  // HTTP 客户端（用于流式对话）

    /**
     * 构造函数：初始化 REST 客户端和流式 HTTP 客户端
     *
     * @param props        LLM 配置属性（baseUrl、apiKey、model）
     * @param objectMapper JSON 序列化器
     */
    public OpenAiChatClient(LlmProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;

        // 1. 获取基础 URL（默认使用阿里云 DashScope）
        String base = props.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            base = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }

        // 2. 创建 REST 客户端（用于普通对话）
        this.restClient = RestClient.builder()
                .baseUrl(base)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // 3. 创建流式 HTTP 客户端（用于 SSE 流式对话）
        this.streamHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * 普通对话：发送消息列表，一次性返回完整响应
     *
     * @param model    模型名称（如 qwen-plus，为空则使用配置的默认模型）
     * @param messages 消息列表（每个消息包含 role 和 content）
     * @return AI 回复的完整文本
     */
    public String chat(String model, List<Map<String, String>> messages) {
        // 1. 验证 API Key
        String key = props.getApiKey();
        if (!StringUtils.hasText(key)) {
            throw new BusinessException("未配置大模型 API Key（请设置 app.llm.api-key 环境变量）");
        }
        String useModel = StringUtils.hasText(model) ? model : props.getModel();

        // 2. 构建请求体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", useModel);
        body.put("messages", messages);
        body.put("temperature", 0.6);  // 温度参数，控制随机性

        // 3. 发送 POST 请求到 /chat/completions
        String resp = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .body(body)
                .retrieve()
                .body(String.class);

        // 4. 验证响应
        if (!StringUtils.hasText(resp)) {
            throw new BusinessException("大模型返回为空");
        }
        
        // 5. 解析 JSON 响应，提取 content
        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new BusinessException("大模型返回格式异常");
            }
            String text = content.asText();
            if (!StringUtils.hasText(text)) {
                throw new BusinessException("大模型返回为空");
            }
            return text;
        } catch (Exception e) {
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException("解析大模型响应失败");
        }
    }

    /**
     * 流式对话：请求体带 {@code "stream":true}，按 SSE 解析增量 {@code choices[0].delta.content}
     * 每收到一段文本调用 {@code onChunk}，最后返回完整拼接字符串
     * 
     * 使用场景：实现打字机效果，提升用户体验
     *
     * @param model    模型名称（如 qwen-plus，为空则使用配置的默认模型）
     * @param messages 消息列表（每个消息包含 role 和 content）
     * @param onChunk  每收到一段文本时的回调函数
     * @return 完整拼接的回复文本
     */
    public String chatStream(String model, List<Map<String, String>> messages, Consumer<String> onChunk) {
        // 1. 验证 API Key
        String key = props.getApiKey();
        if (!StringUtils.hasText(key)) {
            throw new BusinessException("未配置大模型 API Key（请设置 app.llm.api-key 或环境变量 APP_LLM_API_KEY）");
        }
        String useModel = StringUtils.hasText(model) ? model : props.getModel();

        // 2. 构建请求 URL
        String base = props.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            base = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        String url = base.replaceAll("/$", "") + "/chat/completions";

        // 3. 构建请求体（stream=true 开启流式模式）
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", useModel);
        body.put("messages", messages);
        body.put("temperature", 0.6);
        body.put("stream", true);  // 关键：开启流式响应

        // 4. 序列化请求体为 JSON
        String bodyJson;
        try {
            bodyJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BusinessException("序列化请求失败");
        }

        // 5. 创建 HTTP 请求（使用 java.net.http.HttpClient）
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))  // 设置 120 秒超时
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();

        // 6. 发送请求并处理 SSE 流式响应
        StringBuilder full = new StringBuilder();
        try {
            HttpResponse<java.io.InputStream> response = streamHttpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            
            // 检查响应状态码
            if (response.statusCode() != 200) {
                String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException("大模型返回错误（" + response.statusCode() + "）：" + truncate(err, 300));
            }
            
            // 7. 逐行读取 SSE 数据
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 只处理以 "data:" 开头的行
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    
                    // 结束标志
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    if (data.isEmpty()) {
                        continue;
                    }
                    
                    // 8. 解析 JSON，提取 delta.content
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                        if (!delta.isMissingNode() && !delta.isNull()) {
                            String piece = delta.asText();
                            if (!piece.isEmpty()) {
                                full.append(piece);       // 拼接到完整文本
                                onChunk.accept(piece);    // 回调通知前端
                            }
                        }
                    } catch (Exception ignored) {
                        // 忽略无法解析的片段
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (java.io.IOException e) {
            // 如果已经收到部分内容，不抛出异常（优雅降级）
            if (full.isEmpty()) {
                throw new BusinessException("流式请求中断：" + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("流式请求被中断");
        }
        return full.toString();
    }

    /**
     * 截断字符串，防止错误信息过长
     *
     * @param s   原始字符串
     * @param max 最大长度
     * @return 截断后的字符串
     */
    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}

