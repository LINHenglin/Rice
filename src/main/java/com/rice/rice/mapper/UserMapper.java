package com.rice.rice.mapper;

import com.rice.rice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 用户数据访问层（Mapper）
 * 基于 Spring Data JPA，继承 JpaRepository 接口
 * 提供用户相关的数据库操作方法
 */
public interface UserMapper extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户对象（Optional，可能为空）
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否已被其他用户使用（排除指定用户 ID）
     * 用于更新用户信息时验证用户名唯一性
     *
     * @param username 用户名
     * @param userId   要排除的用户 ID
     * @return true=已存在，false=不存在
     */
    boolean existsByUsernameAndUserIdNot(String username, Long userId);

    /**
     * 根据关键词搜索用户（用户名模糊匹配，不区分大小写）
     * JPQL 查询：SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :k, '%'))
     *
     * @param keyword  搜索关键词
     * @param pageable 分页参数
     * @return 分页用户列表
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :k, '%'))")
    Page<User> searchByKeyword(@Param("k") String keyword, Pageable pageable);
}
