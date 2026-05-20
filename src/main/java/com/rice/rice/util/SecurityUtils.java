package com.rice.rice.util;

import com.rice.rice.common.BusinessException;
import com.rice.rice.security.RiceUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 * 提供从 Spring Security 上下文获取当前用户信息的便捷方法
 * 所有方法都是静态方法，无需实例化
 */
public final class SecurityUtils {

    /** 私有构造函数，防止实例化 */
    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户详情
     * 从 Spring Security 上下文中提取 RiceUserDetails 对象
     *
     * @return 当前用户详情对象
     * @throws BusinessException 如果用户未登录或认证信息无效
     */
    public static RiceUserDetails requireCurrentUser() {
        // 从 SecurityContext 中获取认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // 验证认证信息和用户类型
        if (auth == null || !(auth.getPrincipal() instanceof RiceUserDetails)) {
            throw new BusinessException("未登录");
        }
        
        return (RiceUserDetails) auth.getPrincipal();
    }

    /**
     * 获取当前登录用户的 ID
     * 便捷方法，等价于 requireCurrentUser().getUserId()
     *
     * @return 当前用户 ID
     * @throws BusinessException 如果用户未登录
     */
    public static Long requireUserId() {
        return requireCurrentUser().getUserId();
    }
}
