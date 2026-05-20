package com.rice.rice.mapper;

import com.rice.rice.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 聊天消息数据访问层（Mapper）
 * 基于 Spring Data JPA，继承 JpaRepository 接口
 * 提供 AI 聊天消息相关的数据库操作方法
 */
public interface ChatMessageMapper extends JpaRepository<ChatMessage, Long> {

    /**
     * 获取指定会话的所有消息（按创建时间升序排列）
     * 用于加载完整的对话历史
     *
     * @param memoryId 会话唯一标识
     * @return 消息列表（按时间从旧到新）
     */
    List<ChatMessage> findByMemoryIdOrderByCreatedAtAsc(String memoryId);

    /**
     * 获取指定会话的最近 20 条消息（按创建时间降序排列）
     * 用于构建对话上下文，限制 token 数量
     *
     * @param memoryId 会话唯一标识
     * @return 消息列表（按时间从新到旧，最多 20 条）
     */
    List<ChatMessage> findTop20ByMemoryIdOrderByCreatedAtDesc(String memoryId);

    /**
     * 删除指定会话的所有消息
     * 方法名规则：deleteBy + MemoryId
     *
     * @param memoryId 会话唯一标识
     */
    void deleteByMemoryId(String memoryId);
}
