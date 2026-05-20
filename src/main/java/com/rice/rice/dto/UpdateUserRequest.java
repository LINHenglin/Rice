package com.rice.rice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更新用户信息请求 DTO
 * 用于用户修改自己的个人信息（支持部分字段更新）
 */
public class UpdateUserRequest {

    /** 用户 ID（必填，只能修改自己的信息） */
    @NotNull
    private Long userId;

    /** 用户名（可选，如果提供则必须唯一） */
    private String username;
    
    /** 头像 URL（可选） */
    private String avatarUrl;
    
    /** 当前密码（修改密码时必填） */
    private String password;
    
    /** 新密码（修改密码时必填，至少6位） */
    private String newPassword;

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
     * @return 用户名，可能为 null
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
     * 获取当前密码
     *
     * @return 当前密码，可能为 null
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置当前密码
     *
     * @param password 当前密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取新密码
     *
     * @return 新密码，可能为 null
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * 设置新密码
     *
     * @param newPassword 新密码
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
