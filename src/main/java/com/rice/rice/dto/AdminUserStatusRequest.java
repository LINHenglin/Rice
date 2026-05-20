package com.rice.rice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员更新用户状态请求 DTO
 * 用于启用或禁用用户账号
 */
public class AdminUserStatusRequest {

    /** 用户 ID（必填） */
    @NotNull
    private Long userId;

    /** 是否禁用（true=禁用，false=启用）（必填） */
    @NotNull
    private Boolean disabled;

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
}
