package com.rice.rice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rice.rice.common.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * REST 认证入口点
 * 当用户未认证或认证失败时触发（HTTP 401 Unauthorized）
 * 返回统一的 JSON 格式错误响应
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;  // JSON 序列化器

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理认证失败
     * 设置 HTTP 401 状态码，返回 JSON 格式的错误信息
     *
     * @param request         HTTP 请求
     * @param response        HTTP 响应
     * @param authException   认证异常
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        // 设置响应状态码和类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 Unauthorized
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 返回统一的错误响应
        objectMapper.writeValue(response.getOutputStream(), ApiResult.fail("请先登录"));
    }
}
