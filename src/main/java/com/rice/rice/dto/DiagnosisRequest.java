package com.rice.rice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 病害诊断请求 DTO
 * 用于接收前端提交的识别请求数据
 */
public class DiagnosisRequest {

    private String riceVariety;   // 水稻品种（可选）

    private String symptomDesc;   // 症状描述（可选）

    @NotBlank(message = "图片 URL 不能为空")
    private String imageUrl;      // 图片 URL（必填）

    public String getRiceVariety() {
        return riceVariety;
    }

    public void setRiceVariety(String riceVariety) {
        this.riceVariety = riceVariety;
    }

    public String getSymptomDesc() {
        return symptomDesc;
    }

    public void setSymptomDesc(String symptomDesc) {
        this.symptomDesc = symptomDesc;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
