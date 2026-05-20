package com.rice.rice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rice.rice.common.ApiResult;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证过滤器
 * 拦截所有请求，验证 JWT Token，设置 Spring Security 上下文
 * 继承 OncePerRequestFilter 确保每个请求只执行一次
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;                        // JWT 工具类
    private final RiceUserDetailsService userDetailsService; // 用户详情服务
    private final ObjectMapper objectMapper;              // JSON 序列化器

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RiceUserDetailsService userDetailsService, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    /**
     * 过滤器的核心逻辑：解析 JWT Token 并设置认证信息
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头中获取 Authorization
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header)) {
            // 没有 Token，直接放行（由后续权限控制决定是否需要认证）
            filterChain.doFilter(request, response);
            return;
        }
        
        // 2. 提取 Token（兼容 "Bearer <token>" 和直接传 token 两种格式）
        String token = header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 3. 解析 Token，获取 Claims
            var claims = jwtUtil.parse(token);
            
            // 4. 优先使用 userId 加载用户（稳定主键，避免用户名修改后 Token 失效）
            Object uidObj = claims.get("userId");
            Long userId = null;
            if (uidObj instanceof Number n) {
                userId = n.longValue();
            } else if (uidObj instanceof String s && StringUtils.hasText(s)) {
                userId = Long.parseLong(s);
            }

            // 5. 如果当前没有认证信息，则进行认证
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user;
                if (userId != null) {
                    // 通过 userId 加载用户（推荐方式）
                    user = userDetailsService.loadUserByUserId(userId);
                } else {
                    // 降级方案：通过 username 加载用户
                    String username = claims.getSubject();
                    if (!StringUtils.hasText(username)) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    user = userDetailsService.loadUserByUsername(username);
                }
                
                // 6. 检查账号是否被禁用
                if (!user.isEnabled()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    ApiResult<Void> body = ApiResult.fail("账号已被禁用");
                    objectMapper.writeValue(response.getOutputStream(), body);
                    return;
                }
                
                // 7. 创建认证对象并设置到 SecurityContext
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (UsernameNotFoundException e) {
            // 用户不存在（可能已被删除）
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ApiResult<Void> body = ApiResult.fail("用户不存在");
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        } catch (JwtException | NumberFormatException e) {
            // Token 无效或解析失败
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ApiResult<Void> body = ApiResult.fail("登录已过期");
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }
        
        // 9. 继续执行后续过滤器
        filterChain.doFilter(request, response);
    }
}
