package com.rice.rice.mapper;

import com.rice.rice.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 聊天会话数据访问层（Mapper）
 * 基于 Spring Data JPA，继承 JpaRepository 接口
 * 提供 AI 聊天会话相关的数据库操作方法
 */
public interface ChatSessionMapper extends JpaRepository<ChatSession, Long> {

    /**
     * 根据会话 ID 和用户 ID 查找会话（确保用户只能访问自己的会话）
     *
     * @param memoryId 会话唯一标识
     * @param userId   用户 ID
     * @return 会话对象（Optional，可能为空）
     */
    Optional<ChatSession> findByMemoryIdAndUserId(String memoryId, Long userId);

    /**
     * 分页查询指定用户的会话列表（按创建时间降序排列）
     *
     * @param userId   用户 ID
     * @param pageable 分页参数
     * @return 分页会话列表
     */
    Page<ChatSession> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据会话 ID 查找会话（不限制用户）
     * 通常用于内部服务调用
     *
     * @param memoryId 会话唯一标识
     * @return 会话对象（Optional，可能为空）
     */
    Optional<ChatSession> findByMemoryId(String memoryId);

    /**
     * 删除指定会话的所有记录
     *
     * @param memoryId 会话唯一标识
     */
    void deleteByMemoryId(String memoryId);

    /**
     * 删除指定用户的所有聊天会话
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);
}
