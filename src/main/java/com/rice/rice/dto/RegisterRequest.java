package com.rice.rice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户注册请求 DTO
 * 用于新用户注册接口
 */
public class RegisterRequest {

    /** 用户名（必填，不能为空） */
    @NotBlank
    private String username;

    /** 密码（必填，不能为空，最小长度 6 位） */
    @NotBlank
    private String password;

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
     * 获取密码
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
