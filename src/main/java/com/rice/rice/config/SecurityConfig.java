package com.rice.rice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rice.rice.security.JwtAuthenticationFilter;
import com.rice.rice.security.RestAccessDeniedHandler;
import com.rice.rice.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置类
 * 配置认证、授权、JWT 过滤器、CORS 等安全相关功能
 * 
 * 主要功能：
 * 1. 密码加密（BCrypt）
 * 2. JWT Token 认证
 * 3. 接口权限控制
 * 4. CORS 跨域配置
 */
@Configuration
@EnableWebSecurity          // 启用 Spring Security Web 安全功能
@EnableMethodSecurity       // 启用方法级别的安全注解（如 @PreAuthorize）
public class SecurityConfig {

    /**
     * 密码编码器 Bean
     * 使用 BCrypt 算法进行密码加密，安全性高且自动加盐
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JSON 序列化器 Bean
     * 用于将对象转换为 JSON 字符串（异常处理、SSE 流式响应等场景使用）
     *
     * @return ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 认证管理器 Bean
     * 配置用户认证逻辑，使用 DaoAuthenticationProvider 从数据库加载用户信息
     *
     * @param userDetailsService 用户详情服务（从数据库加载用户）
     * @param passwordEncoder    密码编码器（用于验证密码）
     * @return AuthenticationManager 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        // 创建 DAO 认证提供者
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);  // 设置密码编码器
        return new ProviderManager(provider);            // 创建认证管理器
    }

    /**
     * 安全过滤器链配置
     * 定义 HTTP 安全策略，包括 CSRF、CORS、会话管理、权限控制等
     *
     * @param http                      HTTP 安全构建器
     * @param jwtAuthenticationFilter   JWT 认证过滤器
     * @param authenticationEntryPoint  认证失败处理器（401）
     * @param accessDeniedHandler       权限拒绝处理器（403）
     * @return SecurityFilterChain 安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // 1. 禁用 CSRF（因为使用 JWT，不需要 CSRF 保护）
                .csrf(csrf -> csrf.disable())
                
                // 2. 配置 CORS 跨域，允许前端跨域访问后端 API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 3. 无状态会话（不使用 Session，每次请求都需要 JWT Token）
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 4. 配置异常处理
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)   // 认证失败处理（401）
                        .accessDeniedHandler(accessDeniedHandler))            // 权限拒绝处理（403）
                
                // 5. 配置接口访问权限
                .authorizeHttpRequests(a -> a
                        // 允许 OPTIONS 预检请求（CORS 需要）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 允许公开访问：登录、注册
                        .requestMatchers("/user/user/login", "/user/user/register").permitAll()
                        // 允许公开访问：上传的图片文件
                        .requestMatchers("/uploads/**").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated())
                
                // 6. 在用户名密码认证过滤器之前添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    /**
     * CORS 跨域配置
     * 允许前端跨域访问后端 API
     *
     * @return CorsConfigurationSource CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        
        // 允许的源（* 表示所有域名，生产环境建议指定具体域名）
        c.setAllowedOriginPatterns(List.of("*"));
        
        // 允许的 HTTP 方法
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 允许的请求头（* 表示所有）
        c.setAllowedHeaders(List.of("*"));
        
        // 允许携带凭证（Cookie、Authorization 头等）
        c.setAllowCredentials(true);
        
        // 注册 CORS 配置到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
