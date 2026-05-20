package com.rice.rice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务类
 * 负责图片上传、存储和生成访问 URL
 * 使用阿里云 OSS 进行文件存储
 */
@Service
public class FileStorageService {

    private final AliyunOssStorageService aliyunOssStorageService; // 阿里云 OSS 存储服务

    /**
     * 构造函数：初始化文件存储配置
     *
     * @param aliyunOssStorageService   阿里云 OSS 存储服务
     */
    public FileStorageService(AliyunOssStorageService aliyunOssStorageService) {
        this.aliyunOssStorageService = aliyunOssStorageService;
    }

    /**
     * 存储图片文件
     * 流程：验证文件 → 生成唯一文件名 → 上传到 OSS → 返回访问 URL
     * 默认存储到 detections 文件夹（叶片识别图片）
     *
     * @param file 上传的图片文件
     * @return 可访问的图片 URL
     */
    public String storeImage(MultipartFile file) {
        // 默认使用 detections 文件夹（叶片识别图片）
        return aliyunOssStorageService.storeImage(file, "detections");
    }

    /**
     * 存储图片文件到指定文件夹
     * 流程：验证文件 → 生成唯一文件名 → 上传到 OSS → 返回访问 URL
     *
     * @param file       上传的图片文件
     * @param folderName 文件夹名称（avatars: 头像, detections: 叶片识别）
     * @return 可访问的图片 URL
     */
    public String storeImage(MultipartFile file, String folderName) {
        return aliyunOssStorageService.storeImage(file, folderName);
    }

    /**
     * 根据 URL 删除文件
     * 从阿里云 OSS 中删除文件
     *
     * @param fileUrl 文件的完整 URL
     */
    public void deleteFileByUrl(String fileUrl) {
        aliyunOssStorageService.deleteFileByUrl(fileUrl);
    }
}
