package com.rice.rice.controller;

import com.rice.rice.common.ApiResult;
import com.rice.rice.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片控制器
 * 提供通用的图片上传接口，无需特殊权限即可访问
 * 路径前缀：/common/file
 */
@RestController
@RequestMapping("/common/file")
public class ImageController {

    private final FileStorageService fileStorageService;  // 文件存储服务

    public ImageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传图片接口（默认存储到 detections 文件夹）
     * POST /common/file/upload/image
     * 
     * 使用 multipart/form-data 格式上传，字段名为 "image"
     * 支持 JPG、PNG、GIF、WEBP、BMP 格式
     *
     * @param image 上传的图片文件
     * @return 图片访问 URL
     */
    @PostMapping("/upload/image")
    public ApiResult<String> uploadImage(@RequestPart("image") MultipartFile image) {
        // 调用文件存储服务保存图片，返回可访问的 URL
        return ApiResult.ok(fileStorageService.storeImage(image));
    }

    /**
     * 上传头像接口
     * POST /common/file/upload/avatar
     * 
     * 使用 multipart/form-data 格式上传，字段名为 "avatar"
     * 支持 JPG、PNG、GIF、WEBP、BMP 格式
     * 图片将存储到 avatars 文件夹
     *
     * @param avatar 上传的头像文件
     * @return 头像访问 URL
     */
    @PostMapping("/upload/avatar")
    public ApiResult<String> uploadAvatar(@RequestPart("avatar") MultipartFile avatar) {
        // 调用文件存储服务保存头像到 avatars 文件夹
        return ApiResult.ok(fileStorageService.storeImage(avatar, "avatars"));
    }
}
