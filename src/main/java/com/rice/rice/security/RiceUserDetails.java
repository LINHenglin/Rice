package com.rice.rice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义用户详情类
 * 实现 Spring Security 的 UserDetails 接口
 * 封装用户认证信息和权限信息
 */
public class RiceUserDetails implements UserDetails {

    private final Long userId;       // 用户 ID
    private final String username;   // 用户名
    private final String password;   // 密码（BCrypt 加密）
    private final String role;       // 角色（user/admin）
    private final boolean disabled;  // 是否禁用

    /**
     * 构造函数
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param password 密码（BCrypt 加密）
     * @param role     角色（user/admin）
     * @param disabled 是否禁用
     */
    public RiceUserDetails(Long userId, String username, String password, String role, boolean disabled) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role != null ? role : "user";  // 默认角色为 user
        this.disabled = disabled;
    }

    /**
     * 获取用户 ID
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取角色
     *
     * @return 角色（user 或 admin）
     */
    public String getRole() {
        return role;
    }

    /**
     * 获取用户权限列表
     * Spring Security 要求权限以 "ROLE_" 开头
     *
     * @return 权限集合（如 [ROLE_USER] 或 [ROLE_ADMIN]）
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String r = "ROLE_" + role.toUpperCase();
        return List.of(new SimpleGrantedAuthority(r));
    }

    /**
     * 获取密码（BCrypt 加密后的密文）
     *
     * @return 加密后的密码
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账号是否未过期（始终返回 true）
     *
     * @return true
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号是否未锁定（disabled=false 表示未锁定）
     *
     * @return true=未锁定，false=已锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return !disabled;
    }

    /**
     * 凭证是否未过期（始终返回 true）
     *
     * @return true
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账号是否启用（disabled=false 表示启用）
     *
     * @return true=启用，false=禁用
     */
    @Override
    public boolean isEnabled() {
        return !disabled;
    }
}
