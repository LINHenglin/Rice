package com.rice.rice.dto;

import java.util.Map;

/**
 * 水稻病害识别结果 DTO
 * 由 Python 深度学习模型返回的预测结果
 */
public class DiseasePredictionResult {
    
    /** 预测类别的英文名称（如 "blast"、"brown_spot"） */
    private String classEn;
    
    /** 预测类别的中文名称（如 "稻瘟病"、"褐斑病"） */
    private String classCn;
    
    /** 预测置信度（0~1之间，值越高表示越确信） */
    private Double confidence;
    
    /** 所有类别的概率分布 Map（类别名 → 概率值） */
    private Map<String, Double> allProbs;
    
    /** 防治建议文本（包含症状描述、治疗方法等） */
    private String treatment;

    /**
     * 获取预测类别的英文名称
     *
     * @return 英文类别名（如 "blast"、"brown_spot"）
     */
    public String getClassEn() {
        return classEn;
    }

    /**
     * 设置预测类别的英文名称
     *
     * @param classEn 英文类别名
     */
    public void setClassEn(String classEn) {
        this.classEn = classEn;
    }

    /**
     * 获取预测类别的中文名称
     *
     * @return 中文类别名（如 "稻瘟病"、"褐斑病"）
     */
    public String getClassCn() {
        return classCn;
    }

    /**
     * 设置预测类别的中文名称
     *
     * @param classCn 中文类别名
     */
    public void setClassCn(String classCn) {
        this.classCn = classCn;
    }

    /**
     * 获取预测置信度
     *
     * @return 置信度值（0~1之间）
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * 设置预测置信度
     *
     * @param confidence 置信度值（0~1之间）
     */
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    /**
     * 获取所有类别的概率分布
     *
     * @return 概率分布 Map（类别名 → 概率值）
     */
    public Map<String, Double> getAllProbs() {
        return allProbs;
    }

    /**
     * 设置所有类别的概率分布
     *
     * @param allProbs 概率分布 Map
     */
    public void setAllProbs(Map<String, Double> allProbs) {
        this.allProbs = allProbs;
    }

    /**
     * 获取防治建议
     *
     * @return 防治建议文本
     */
    public String getTreatment() {
        return treatment;
    }

    /**
     * 设置防治建议
     *
     * @param treatment 防治建议文本
     */
    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }
}
