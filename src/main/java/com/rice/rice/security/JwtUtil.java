package com.rice.rice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT (JSON Web Token) 工具类
 * 负责 Token 的生成和解析，用于用户身份认证
 * 
 * 使用 HS256 算法签名，密钥长度至少 32 字节
 */
@Component
public class JwtUtil {

    private final SecretKey key;          // JWT 签名密钥
    private final long expirationMs;      // Token 有效期（毫秒）

    /**
     * 构造函数：初始化 JWT 密钥和有效期
     *
     * @param secret       签名字符串（从配置文件读取，至少 32 字节）
     * @param expirationMs Token 有效期（毫秒），默认 24 小时
     */
    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // 将字符串转换为 UTF-8 字节数组
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        // 生成 HMAC-SHA 密钥
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    /**
     * 创建 JWT Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     用户角色（user/admin）
     * @return JWT Token 字符串
     */
    public String createToken(Long userId, String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);  // 计算过期时间
        return Jwts.builder()
                .subject(username)                          // 设置主题（用户名）
                .claim("userId", userId)                    // 自定义声明：用户 ID
                .claim("role", role != null ? role : "user") // 自定义声明：角色
                .issuedAt(now)                              // 签发时间
                .expiration(exp)                            // 过期时间
                .signWith(key)                              // 使用密钥签名
                .compact();                                 // 生成紧凑的 Token 字符串
    }

    /**
     * 解析 JWT Token，提取 Claims 信息
     *
     * @param token JWT Token 字符串
     * @return Claims 对象（包含用户信息）
     * @throws io.jsonwebtoken.JwtException Token 无效或已过期时抛出异常
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)        // 使用密钥验证签名
                .build()
                .parseSignedClaims(token) // 解析签名的 Claims
                .getPayload();          // 获取载荷数据
    }
}
