package com.rice.rice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员删除用户请求 DTO
 * 用于删除用户账号
 */
public class DeleteUserRequest {

    /** 用户 ID（必填） */
    @NotNull
    private Long userId;

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
}
