package com.rice.rice.service;

import com.rice.rice.common.BusinessException;
import com.rice.rice.dto.AdminUserStatusRequest;
import com.rice.rice.dto.PageQuery;
import com.rice.rice.dto.PagedData;
import com.rice.rice.dto.ResetPasswordRequest;
import com.rice.rice.entity.DetectionRecord;
import com.rice.rice.entity.User;
import com.rice.rice.mapper.ChatSessionMapper;
import com.rice.rice.mapper.DetectionRecordMapper;
import com.rice.rice.mapper.UserMapper;
import com.rice.rice.util.TimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台服务类
 * 提供系统统计、用户管理等管理员功能
 */
@Service
public class AdminService {

    private final UserMapper userMapper;                    // 用户数据访问层
    private final DetectionRecordMapper detectionRecordMapper; // 检测记录数据访问层
    private final ChatSessionMapper chatSessionMapper;      // 聊天会话数据访问层
    private final FileStorageService fileStorageService;    // 文件存储服务
    private final PasswordEncoder passwordEncoder;          // 密码加密器

    public AdminService(
            UserMapper userMapper,
            DetectionRecordMapper detectionRecordMapper,
            ChatSessionMapper chatSessionMapper,
            FileStorageService fileStorageService,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.detectionRecordMapper = detectionRecordMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取系统统计数据
     * 包含：用户总数、检测记录总数、会话总数、今日检测数
     *
     * @return 统计数据 Map
     */
    public Map<String, Object> stats() {
        // 统计用户总数
        long userCount = userMapper.count();
        
        // 统计检测记录总数
        long detectionCount = detectionRecordMapper.count();
        
        // 统计聊天会话总数
        long chatSessionCount = chatSessionMapper.count();
        
        // 统计今日检测数（从今天 00:00:00 开始）
        LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayDetectionCount = detectionRecordMapper.countSince(start);
        
        // 构建响应数据
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userCount", userCount);              // 用户总数
        m.put("detectionCount", detectionCount);    // 检测记录总数
        m.put("chatSessionCount", chatSessionCount); // 会话总数
        m.put("todayDetectionCount", todayDetectionCount); // 今日检测数
        return m;
    }

    /**
     * 分页查询用户列表
     * 支持关键词搜索（用户名模糊匹配）
     *
     * @param q 分页参数（page、pageSize、keyword）
     * @return 分页用户数据
     */
    public PagedData<Map<String, Object>> userPage(PageQuery q) {
        String kw = q.getKeyword();
        Page<User> page;
        
        // 根据是否有关键词决定查询方式
        if (kw == null || kw.isBlank()) {
            // 无关键词：查询所有用户
            page = userMapper.findAll(PageRequest.of(q.getPage() - 1, q.getPageSize()));
        } else {
            // 有关键词：搜索用户名
            page = userMapper.searchByKeyword(kw.trim(), PageRequest.of(q.getPage() - 1, q.getPageSize()));
        }
        
        // 转换为前端需要的格式
        List<Map<String, Object>> records = new ArrayList<>();
        for (User u : page.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", u.getUserId());
            row.put("username", u.getUsername());
            row.put("avatarUrl", u.getAvatarUrl());
            row.put("role", u.getRole());
            row.put("disabled", u.getDisabled());
            row.put("createTime", TimeFormat.iso(u.getCreateTime()));
            records.add(row);
        }
        return new PagedData<>(records, page.getTotalElements());
    }

    /**
     * 更新用户状态（启用/禁用）
     * 安全限制：不能禁用管理员账号
     *
     * @param req 状态更新请求（包含 userId 和 disabled 状态）
     */
    @Transactional
    public void updateUserStatus(AdminUserStatusRequest req) {
        // 查找目标用户
        User target = userMapper.findById(req.getUserId()).orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 安全检查：禁止禁用管理员账号
        if (Boolean.TRUE.equals(req.getDisabled()) && "admin".equalsIgnoreCase(target.getRole())) {
            throw new BusinessException("不能禁用管理员账号");
        }
        
        // 更新用户状态
        target.setDisabled(req.getDisabled());
        userMapper.save(target);
    }

    /**
     * 删除用户
     * 安全限制：
     * 1. 不能删除管理员账号
     * 2. 会级联删除该用户的检测记录和聊天会话
     * 3. 会删除该用户上传的所有图片（头像、识别图片等）
     *
     * @param userId 要删除的用户 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        // 查找目标用户
        User target = userMapper.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 安全检查：禁止删除管理员账号
        if ("admin".equalsIgnoreCase(target.getRole())) {
            throw new BusinessException("不能删除管理员账号");
        }
        
        // 1. 收集并删除用户的头像
        if (target.getAvatarUrl() != null && !target.getAvatarUrl().isEmpty()) {
            fileStorageService.deleteFileByUrl(target.getAvatarUrl());
        }
        
        // 2. 收集并删除该用户的所有检测记录相关图片
        List<DetectionRecord> records = detectionRecordMapper.findByUserId(userId);
        for (DetectionRecord record : records) {
            // 删除原始识别图片
            if (record.getImageUrl() != null && !record.getImageUrl().isEmpty()) {
                fileStorageService.deleteFileByUrl(record.getImageUrl());
            }
        }
        
        // 3. 级联删除该用户的检测记录
        detectionRecordMapper.deleteByUserId(userId);
        
        // 4. 级联删除该用户的聊天会话（会自动级联删除聊天消息）
        chatSessionMapper.deleteByUserId(userId);
        
        // 5. 删除用户
        userMapper.deleteById(userId);
    }

    /**
     * 重置用户密码
     * 将指定用户的密码重置为默认密码 "123456"
     * 安全限制：不能重置管理员账号的密码
     *
     * @param req 重置密码请求（包含 userId）
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        // 查找目标用户
        User target = userMapper.findById(req.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 安全检查：禁止重置管理员账号的密码
        if ("admin".equalsIgnoreCase(target.getRole())) {
            throw new BusinessException("不能重置管理员账号的密码");
        }
        
        // 将密码重置为 "123456"（BCrypt 加密）
        String defaultPassword = "123456";
        target.setPassword(passwordEncoder.encode(defaultPassword));
        userMapper.save(target);
    }
}
