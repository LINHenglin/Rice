package com.rice.rice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 聊天请求 DTO
 * 用于发送问题到 AI 对话接口
 */
public class ChatRequest {

    /** 会话 ID（可选，不传则创建新会话） */
    private String memoryId;

    /** 用户问题（必填，不能为空） */
    @NotBlank
    private String question;

    /**
     * 获取会话 ID
     *
     * @return 会话 ID，可能为 null
     */
    public String getMemoryId() {
        return memoryId;
    }

    /**
     * 设置会话 ID
     *
     * @param memoryId 会话 ID
     */
    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    /**
     * 获取用户问题
     *
     * @return 问题文本
     */
    public String getQuestion() {
        return question;
    }

    /**
     * 设置用户问题
     *
     * @param question 问题文本
     */
    public void setQuestion(String question) {
        this.question = question;
    }
}
