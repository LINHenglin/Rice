package com.rice.rice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

/**
 * 删除检测记录请求体
 * 与分页列表中的 detectionRecordId 对应
 * 兼容前端可能使用的字段名 "id"
 */
public class DetectionRecordDeleteRequest {

    /** 
     * 检测记录主键 ID（必填）
     * 部分前端可能用字段名 {@code id}，通过 @JsonAlias 一并兼容
     */
    @NotNull
    @JsonAlias("id")
    private Long detectionRecordId;

    /**
     * 获取检测记录 ID
     *
     * @return 检测记录主键 ID
     */
    public Long getDetectionRecordId() {
        return detectionRecordId;
    }

    /**
     * 设置检测记录 ID
     *
     * @param detectionRecordId 检测记录主键 ID
     */
    public void setDetectionRecordId(Long detectionRecordId) {
        this.detectionRecordId = detectionRecordId;
    }
}
