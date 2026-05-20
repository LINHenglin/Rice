package com.rice.rice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmProperties {

    /**
     * OpenAI 兼容接口的 baseUrl（例如通义千问 / OpenAI / 兼容网关）。
     * 例：https://dashscope.aliyuncs.com/compatible-mode/v1
     */
    @Value("${app.llm.base-url}")
    private String baseUrl;

    /**
     * API Key。
     * 生产环境建议用环境变量注入，勿写入代码仓库。
     */
    @Value("${app.llm.api-key}")
    private String apiKey;

    /**
     * 模型名称。
     * 通义千问示例：qwen-plus, qwen-turbo, qwen-max
     */
    @Value("${app.llm.model}")
    private String model;

    /**
     * 发送给模型的系统提示词（用于约束回答风格/安全边界）。
     */
    private String systemPrompt = "你是一个水稻病虫害智能助手。请用中文简体回答，内容清晰、可操作，必要时给出分点建议。";

    /**
     * 取最近 N 条消息作为上下文，避免 prompt 过长。
     */
    private int maxContextMessages = 20;

    /**
     * 获取 API 基础 URL
     * @return API 基础 URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置 API 基础 URL
     * @param baseUrl API 基础 URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 获取 API Key
     * @return API Key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置 API Key
     * @param apiKey API Key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 获取模型名称
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取系统提示词
     * @return 系统提示词
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 设置系统提示词
     * @param systemPrompt 系统提示词
     */
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * 获取最大上下文消息数
     * @return 最大上下文消息数
     */
    public int getMaxContextMessages() {
        return maxContextMessages;
    }

    /**
     * 设置最大上下文消息数
     * @param maxContextMessages 最大上下文消息数
     */
    public void setMaxContextMessages(int maxContextMessages) {
        this.maxContextMessages = maxContextMessages;
    }
}

