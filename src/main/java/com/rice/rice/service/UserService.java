package com.rice.rice.service;

import com.rice.rice.common.BusinessException;
import com.rice.rice.dto.LoginRequest;
import com.rice.rice.dto.RegisterRequest;
import com.rice.rice.dto.UpdateUserRequest;
import com.rice.rice.entity.User;
import com.rice.rice.mapper.UserMapper;
import com.rice.rice.security.JwtUtil;
import com.rice.rice.security.RiceUserDetails;
import com.rice.rice.util.SecurityUtils;
import com.rice.rice.util.TimeFormat;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户服务类
 * 负责用户注册、登录、信息查询和更新等业务逻辑
 */
@Service
public class UserService {

    private final UserMapper userMapper;                      // 用户数据访问层
    private final PasswordEncoder passwordEncoder;            // 密码加密器（BCrypt）
    private final AuthenticationManager authenticationManager; // Spring Security 认证管理器
    private final JwtUtil jwtUtil;                            // JWT Token 工具

    public UserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户登录
     * 流程：验证用户名密码 → 生成 JWT Token
     *
     * @param req 登录请求（包含用户名和密码）
     * @return JWT Token 字符串
     */
    public String login(LoginRequest req) {
        // 去除用户名前后空格
        String username = trimToNull(req.getUsername());
        if (username == null) {
            throw new BusinessException("用户名不能为空");
        }
        String password = req.getPassword() == null ? "" : req.getPassword().trim();
        
        // 先检查用户是否存在
        User entity = userMapper.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 检查账号是否被禁用
        if (Boolean.TRUE.equals(entity.getDisabled())) {
            throw new BusinessException("账号已被禁用");
        }
        
        // 使用 Spring Security 验证密码
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            
            // 获取认证后的用户信息
            RiceUserDetails u = (RiceUserDetails) auth.getPrincipal();
            
            // 生成并返回 JWT Token
            return jwtUtil.createToken(entity.getUserId(), entity.getUsername(), entity.getRole());
        } catch (Exception e) {
            // 密码错误
            throw new BusinessException("密码错误");
        }
    }

    /**
     * 用户注册
     * 流程：验证唯一性 → 加密密码 → 保存用户
     *
     * @param req 注册请求（包含用户名、密码）
     */
    @Transactional
    public void register(RegisterRequest req) {
        String username = trimToNull(req.getUsername());
        
        // 验证必填字段
        if (username == null || username.isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        
        // 检查用户名是否已存在
        if (userMapper.findByUsername(username).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        
        // 创建新用户对象
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(req.getPassword().trim()));  // BCrypt 加密密码
        u.setRole("user");           // 默认角色为普通用户
        u.setDisabled(false);        // 默认未禁用
        userMapper.save(u);          // 保存到数据库
    }

    /**
     * 获取当前登录用户的详细信息
     *
     * @return 用户信息 Map（包含 userId、username 等）
     */
    public Map<String, Object> getUserInfo() {
        Long uid = SecurityUtils.requireUserId();  // 从 JWT Token 中获取当前用户 ID
        User u = userMapper.findById(uid).orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 构建响应数据
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getUserId());
        m.put("username", u.getUsername());
        m.put("avatarUrl", u.getAvatarUrl());
        m.put("createTime", TimeFormat.iso(u.getCreateTime()));
        m.put("role", u.getRole());
        return m;
    }

    /**
     * 更新用户信息
     * 只能修改自己的信息，支持部分字段更新
     * 支持修改：用户名、头像、密码
     *
     * @param req 更新请求（包含要修改的字段）
     * @return 更新后的用户信息
     */
    @Transactional
    public Map<String, Object> updateUser(UpdateUserRequest req) {
        Long currentId = SecurityUtils.requireUserId();
        
        // 权限验证：只能修改自己的信息
        if (!currentId.equals(req.getUserId())) {
            throw new BusinessException("无权修改该用户");
        }
        
        User u = userMapper.findById(req.getUserId()).orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 处理密码修改逻辑
        if (req.getNewPassword() != null && !req.getNewPassword().trim().isEmpty()) {
            // 新密码不为空，需要修改密码
            String newPassword = req.getNewPassword().trim();
            
            // 验证新密码长度
            if (newPassword.length() < 6) {
                throw new BusinessException("新密码至少6位");
            }
            
            // 验证当前密码
            String currentPassword = trimToNull(req.getPassword());
            if (currentPassword == null || currentPassword.isEmpty()) {
                throw new BusinessException("当前密码不能为空");
            }
            
            // 使用 Spring Security 验证当前密码是否正确
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(u.getUsername(), currentPassword));
            } catch (Exception e) {
                throw new BusinessException("当前密码错误");
            }
            
            // 检查新密码是否与当前密码相同
            if (passwordEncoder.matches(newPassword, u.getPassword())) {
                throw new BusinessException("新密码不能与当前密码相同");
            }
            
            // 密码验证通过，更新为新密码
            u.setPassword(passwordEncoder.encode(newPassword));
        }
        
        // 更新用户名（如果提供）
        if (req.getUsername() != null) {
            String name = trimToNull(req.getUsername());
            if (name == null || name.isEmpty()) {
                throw new BusinessException("用户名不能为空");
            }
            // 检查新用户名是否与其他用户冲突
            if (!name.equals(u.getUsername()) && userMapper.existsByUsernameAndUserIdNot(name, u.getUserId())) {
                throw new BusinessException("用户名已存在");
            }
            u.setUsername(name);
        }
        
        // 更新头像 URL（如果提供）
        if (req.getAvatarUrl() != null) {
            u.setAvatarUrl(trimToNull(req.getAvatarUrl()));
        }
        
        userMapper.save(u);  // 保存更新
        return getUserInfo();  // 返回最新用户信息
    }

    /**
     * 工具方法：将字符串去首尾空格，空字符串转为 null
     *
     * @param s 原始字符串
     * @return 去空格后的字符串，如果为空则返回 null
     */
    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
