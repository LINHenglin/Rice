package com.rice.rice.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.rice.rice.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * 阿里云 OSS 文件存储服务类
 * 负责将图片上传到阿里云 OSS
 */
@Service
public class AliyunOssStorageService {

    private final OSS ossClient;              // OSS 客户端
    private final String bucketName;          // Bucket 名称
    private final String endpoint;            // OSS Endpoint

    /**
     * 构造函数：初始化阿里云 OSS 存储配置
     *
     * @param ossClient     OSS 客户端
     * @param bucketName    Bucket 名称
     * @param endpoint      OSS Endpoint
     */
    public AliyunOssStorageService(
            OSS ossClient,
            @Value("${app.oss.bucket-name}") String bucketName,
            @Value("${app.oss.endpoint}") String endpoint) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
    }

    /**
     * 存储图片文件到阿里云 OSS（指定文件夹）
     * 流程：验证文件 → 生成唯一文件名 → 上传到 OSS → 返回访问 URL
     *
     * @param file       上传的图片文件
     * @param folderName 文件夹名称（avatars: 头像, detections: 叶片识别）
     * @return 可访问的图片 URL
     */
    public String storeImage(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }

        // 验证文件夹名称
        if (folderName == null || folderName.isEmpty()) {
            folderName = "detections";
        }

        // 获取原始文件名和扩展名
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }

        // 验证文件类型，如果不是常见图片格式则默认为 .jpg
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
            ext = ".jpg";
        }

        // 生成唯一文件名（UUID + 扩展名）
        String fileName = folderName + "/" + UUID.randomUUID() + ext;

        try {
            // 上传文件到 OSS
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, 
                    fileName, 
                    file.getInputStream()
            );
            ossClient.putObject(putObjectRequest);
        } catch (IOException e) {
            throw new BusinessException("上传文件到 OSS 失败: " + e.getMessage());
        }

        // 生成可被前端直接访问的 URL
        // 例如：https://bucket-name.oss-cn-hangzhou.aliyuncs.com/avatars/xxxxx.jpg
        String url = String.format("https://%s.%s/%s", bucketName, endpoint.replaceFirst("^https?://", ""), fileName);
        return url;
    }

    /**
     * 根据 URL 删除阿里云 OSS 上的文件
     *
     * @param fileUrl 文件的完整 URL
     */
    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        
        try {
            // 从 URL 中提取对象键（如 avatars/xxx.jpg）
            String prefix = String.format("https://%s.%s/", bucketName, endpoint.replaceFirst("^https?://", ""));
            if (!fileUrl.startsWith(prefix)) {
                // 不是本 Bucket 的文件，忽略
                return;
            }
            
            String objectKey = fileUrl.substring(prefix.length());
            
            // 删除 OSS 上的文件
            ossClient.deleteObject(bucketName, objectKey);
        } catch (Exception e) {
            // 删除失败只记录日志，不抛出异常（避免影响主流程）
            System.err.println("删除 OSS 文件失败: " + fileUrl + ", 错误: " + e.getMessage());
        }
    }
}
