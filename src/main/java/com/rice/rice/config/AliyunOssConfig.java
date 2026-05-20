package com.rice.rice.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置类
 * 负责创建和配置 OSS 客户端
 */
@Configuration
public class AliyunOssConfig {

    /** OSS Endpoint（地域节点） */
    @Value("${app.oss.endpoint:}")
    private String endpoint;

    /** Access Key ID */
    @Value("${app.oss.access-key-id:}")
    private String accessKeyId;

    /** Access Key Secret */
    @Value("${app.oss.access-key-secret:}")
    private String accessKeySecret;

    /** Bucket 名称 */
    @Value("${app.oss.bucket-name:}")
    private String bucketName;

    /**
     * 创建 OSS 客户端 Bean
     * 如果未配置 OSS 参数，则返回 null，系统将使用本地存储
     *
     * @return OSS 客户端实例
     */
    @Bean
    public OSS ossClient() {
        // 如果未配置 OSS 参数，返回 null
        if (endpoint == null || endpoint.isEmpty() ||
            accessKeyId == null || accessKeyId.isEmpty() ||
            accessKeySecret == null || accessKeySecret.isEmpty() ||
            bucketName == null || bucketName.isEmpty()) {
            return null;
        }
        
        // 创建 OSS 客户端
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 获取 Bucket 名称
     *
     * @return Bucket 名称
     */
    @Bean
    public String ossBucketName() {
        return bucketName;
    }
}
