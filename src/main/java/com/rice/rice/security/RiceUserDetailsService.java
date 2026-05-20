package com.rice.rice.security;

import com.rice.rice.entity.User;
import com.rice.rice.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务类
 * 实现 Spring Security 的 UserDetailsService 接口
 * 负责从数据库加载用户信息并转换为 UserDetails 对象
 */
@Service
public class RiceUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;  // 用户数据访问层

    public RiceUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 根据用户名加载用户详情（Spring Security 标准方法）
     * 用于登录认证时验证用户名和密码
     *
     * @param username 用户名
     * @return 用户详情对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userMapper.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return new RiceUserDetails(
                u.getUserId(),
                u.getUsername(),
                u.getPassword(),
                u.getRole(),
                Boolean.TRUE.equals(u.getDisabled()));
    }

    /**
     * 根据用户 ID 加载用户详情（自定义方法）
     * 用于 JWT Token 解析后通过 userId 加载用户（更稳定）
     *
     * @param userId 用户 ID
     * @return 用户详情对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException {
        User u = userMapper.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return new RiceUserDetails(
                u.getUserId(),
                u.getUsername(),
                u.getPassword(),
                u.getRole(),
                Boolean.TRUE.equals(u.getDisabled()));
    }
}
