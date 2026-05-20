package com.rice.rice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员重置用户密码请求 DTO
 */
public class ResetPasswordRequest {

    /** 用户 ID（必填） */
    @NotNull(message = "用户ID不能为空")
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
