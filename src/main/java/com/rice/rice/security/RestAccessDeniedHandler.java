package com.rice.rice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rice.rice.common.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * REST 权限拒绝处理器
 * 当用户已认证但没有访问权限时触发（HTTP 403 Forbidden）
 * 返回统一的 JSON 格式错误响应
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;  // JSON 序列化器

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理权限拒绝异常
     * 设置 HTTP 403 状态码，返回 JSON 格式的错误信息
     *
     * @param request               HTTP 请求
     * @param response              HTTP 响应
     * @param accessDeniedException 权限拒绝异常
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        // 设置响应状态码和类型
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403 Forbidden
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 返回统一的错误响应
        objectMapper.writeValue(response.getOutputStream(), ApiResult.fail("无权限"));
    }
}
