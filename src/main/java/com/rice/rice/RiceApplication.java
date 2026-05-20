package com.rice.rice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 水稻病虫害识别系统 - Spring Boot 应用启动类
 * 
 * 系统功能：
 * 1. 水稻病害图像识别（调用 Python 深度学习模型）
 * 2. AI 智能对话（集成通义千问大语言模型）
 * 3. 用户管理与权限控制（JWT 认证）
 * 4. 检测记录与聊天历史管理
 */
@SpringBootApplication
public class RiceApplication {

    /**
     * 应用入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RiceApplication.class, args);
    }

}
