package com.rice.rice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：sys_user
 * 存储系统用户的基本信息和认证数据
 */
@Entity
@Table(name = "sys_user")
public class User {

    /** 用户 ID（主键，自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /** 用户名（唯一，不能为空，最长 64 字符） */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** 密码（BCrypt 加密存储，不能为空，最长 255 字符） */
    @Column(nullable = false, length = 255)
    private String password;

    /** 头像 URL（可选，最长 512 字符） */
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /** 角色（user/admin，默认 user，最长 32 字符） */
    @Column(length = 32)
    private String role = "user";

    /** 是否禁用（默认 false，true=禁用，false=启用） */
    @Column(nullable = false)
    private Boolean disabled = false;

    /** 创建时间（默认当前时间，不能为空） */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

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
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码（BCrypt 加密后的密文）
     *
     * @return 加密后的密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码（应传入 BCrypt 加密后的密文）
     *
     * @param password 加密后的密码
     */
    public void setPassword(String password) {
        this.password = password;
    }



    /**
     * 获取头像 URL
     *
     * @return 头像 URL，可能为 null
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * 设置头像 URL
     *
     * @param avatarUrl 头像 URL
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 获取角色
     *
     * @return 角色（user 或 admin）
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置角色
     *
     * @param role 角色（user 或 admin）
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取禁用状态
     *
     * @return true=禁用，false=启用
     */
    public Boolean getDisabled() {
        return disabled;
    }

    /**
     * 设置禁用状态
     *
     * @param disabled true=禁用，false=启用
     */
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
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
}
