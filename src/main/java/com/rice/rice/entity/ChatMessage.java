package com.rice.rice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * AI 聊天消息实体类
 * 对应数据库表：chat_message
 * 存储会话中的每一条消息（用户问题或 AI 回答）
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    /** 消息 ID（主键，自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话 ID（关联 chat_session 表，不能为空，最长 64 字符） */
    @Column(name = "memory_id", nullable = false, length = 64)
    private String memoryId;

    /** 角色（user=用户，assistant=AI，不能为空，最长 32 字符） */
    @Column(nullable = false, length = 32)
    private String role;

    /** 消息内容（LONGTEXT 类型，不能为空） */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /** 创建时间（默认当前时间，不能为空） */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 获取消息 ID
     *
     * @return 消息 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置消息 ID
     *
     * @param id 消息 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取会话 ID
     *
     * @return 会话 ID
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
     * 获取角色
     *
     * @return 角色（user 或 assistant）
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置角色
     *
     * @param role 角色（user 或 assistant）
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取消息内容
     *
     * @return 消息文本
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容
     *
     * @param content 消息文本
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
