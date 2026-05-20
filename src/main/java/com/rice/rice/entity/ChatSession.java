package com.rice.rice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * AI 聊天会话实体类
 * 对应数据库表：chat_session
 * 存储用户与 AI 的对话会话信息
 */
@Entity
@Table(name = "chat_session")
public class ChatSession {

    /** 会话记录 ID（主键，自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_recode_id")
    private Long chatRecodeId;

    /** 会话唯一标识（UUID，不能为空，唯一，最长 64 字符） */
    @Column(name = "memory_id", nullable = false, unique = true, length = 64)
    private String memoryId;

    /** 用户 ID（关联 sys_user 表，不能为空） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 第一个问题（TEXT 类型，用于显示会话标题） */
    @Column(name = "first_question", columnDefinition = "TEXT")
    private String firstQuestion;

    /** 创建时间（默认当前时间，不能为空） */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    /** 过期时间（可选，用于自动清理过期会话） */
    @Column(name = "expiration_time")
    private LocalDateTime expirationTime;

    /**
     * 获取会话记录 ID
     *
     * @return 会话记录 ID
     */
    public Long getChatRecodeId() {
        return chatRecodeId;
    }

    /**
     * 设置会话记录 ID
     *
     * @param chatRecodeId 会话记录 ID
     */
    public void setChatRecodeId(Long chatRecodeId) {
        this.chatRecodeId = chatRecodeId;
    }

    /**
     * 获取会话唯一标识
     *
     * @return 会话 ID（UUID）
     */
    public String getMemoryId() {
        return memoryId;
    }

    /**
     * 设置会话唯一标识
     *
     * @param memoryId 会话 ID（UUID）
     */
    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    /**
     * 获取用户 ID
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID
     *
     * @param userId 用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取第一个问题（会话标题）
     *
     * @return 第一个问题，可能为 null
     */
    public String getFirstQuestion() {
        return firstQuestion;
    }

    /**
     * 设置第一个问题
     *
     * @param firstQuestion 第一个问题
     */
    public void setFirstQuestion(String firstQuestion) {
        this.firstQuestion = firstQuestion;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取过期时间
     *
     * @return 过期时间，可能为 null
     */
    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    /**
     * 设置过期时间
     *
     * @param expirationTime 过期时间
     */
    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }
}
