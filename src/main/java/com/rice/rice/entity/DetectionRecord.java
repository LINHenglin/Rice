package com.rice.rice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 水稻病害检测记录实体类
 * 对应数据库表：detection_record
 * 存储用户上传的水稻图片及 AI 诊断结果
 */
@Entity
@Table(name = "detection_record")
public class DetectionRecord {

    /** 检测记录 ID（主键，自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detection_record_id")
    private Long detectionRecordId;

    /** 用户 ID（关联 sys_user 表，不能为空） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 水稻品种（可选，最长 128 字符） */
    @Column(name = "rice_variety", length = 128)
    private String riceVariety;

    /** 症状描述（可选，TEXT 类型） */
    @Column(name = "symptom_desc", columnDefinition = "TEXT")
    private String symptomDesc;

    /** 原始图片 URL（最长 1024 字符） */
    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    /** AI 分析结果（Markdown 格式，LONGTEXT 类型） */
    @Column(name = "analysis_result", columnDefinition = "LONGTEXT")
    private String analysisResult;

    /** 会话 ID（关联 chat_session 表，最长 64 字符） */
    @Column(name = "memory_id", length = 64)
    private String memoryId;

    /** 诊断结论（最长 512 字符） */
    @Column(name = "diagnosis", length = 512)
    private String diagnosis;

    /** 创建时间（默认当前时间，不能为空） */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 获取检测记录 ID
     *
     * @return 检测记录 ID
     */
    public Long getDetectionRecordId() {
        return detectionRecordId;
    }

    /**
     * 设置检测记录 ID
     *
     * @param detectionRecordId 检测记录 ID
     */
    public void setDetectionRecordId(Long detectionRecordId) {
        this.detectionRecordId = detectionRecordId;
    }

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
     * 获取水稻品种
     *
     * @return 水稻品种，可能为 null
     */
    public String getRiceVariety() {
        return riceVariety;
    }

    /**
     * 设置水稻品种
     *
     * @param riceVariety 水稻品种
     */
    public void setRiceVariety(String riceVariety) {
        this.riceVariety = riceVariety;
    }

    /**
     * 获取症状描述
     *
     * @return 症状描述，可能为 null
     */
    public String getSymptomDesc() {
        return symptomDesc;
    }

    /**
     * 设置症状描述
     *
     * @param symptomDesc 症状描述
     */
    public void setSymptomDesc(String symptomDesc) {
        this.symptomDesc = symptomDesc;
    }

    /**
     * 获取原始图片 URL
     *
     * @return 图片 URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 设置原始图片 URL
     *
     * @param imageUrl 图片 URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 获取 AI 分析结果（Markdown 格式）
     *
     * @return 分析结果文本
     */
    public String getAnalysisResult() {
        return analysisResult;
    }

    /**
     * 设置 AI 分析结果
     *
     * @param analysisResult 分析结果文本
     */
    public void setAnalysisResult(String analysisResult) {
        this.analysisResult = analysisResult;
    }

    /**
     * 获取会话 ID
     *
     * @return 会话 ID，可能为 null
     */
    public String getMemoryId() {
        return memoryId;
    }

    /**
     * 设置会话 ID
     *
     * @param memoryId 会话 ID
     */
    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    /**
     * 获取诊断结论
     *
     * @return 诊断结论，可能为 null
     */
    public String getDiagnosis() {
        return diagnosis;
    }

    /**
     * 设置诊断结论
     *
     * @param diagnosis 诊断结论
     */
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
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
