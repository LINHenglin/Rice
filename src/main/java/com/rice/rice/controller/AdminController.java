package com.rice.rice.controller;

import com.rice.rice.common.ApiResult;
import com.rice.rice.dto.AdminUserStatusRequest;
import com.rice.rice.dto.DeleteUserRequest;
import com.rice.rice.dto.PageQuery;
import com.rice.rice.dto.PagedData;
import com.rice.rice.dto.ResetPasswordRequest;
import com.rice.rice.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台控制器
 * 提供系统统计、用户管理等管理员专用接口
 * 所有接口都需要 ADMIN 角色权限
 * 路径前缀：/admin
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")  // 所有方法都需要管理员权限
public class AdminController {

    private final AdminService adminService;  // 管理服务层

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 获取系统统计数据
     * GET /admin/stats
     * 包含：用户总数、检测记录数、会话数、今日检测数
     *
     * @return 统计数据 Map
     */
    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> stats() {
        return ApiResult.ok(adminService.stats());
    }

    /**
     * 分页查询用户列表
     * POST /admin/user/pageQuery
     * 支持关键词搜索（用户名）
     *
     * @param q 分页参数（page、pageSize、keyword）
     * @return 分页用户数据
     */
    @PostMapping("/user/pageQuery")
    public ApiResult<PagedData<Map<String, Object>>> userPage(@Valid @RequestBody PageQuery q) {
        return ApiResult.ok(adminService.userPage(q));
    }

    /**
     * 更新用户状态（启用/禁用）
     * POST /admin/user/status
     * 注意：不能禁用管理员账号
     *
     * @param req 状态更新请求（userId、disabled）
     * @return 成功提示
     */
    @PostMapping("/user/status")
    public ApiResult<Void> userStatus(@Valid @RequestBody AdminUserStatusRequest req) {
        adminService.updateUserStatus(req);
        return ApiResult.ok(null);
    }

    /**
     * 删除用户
     * POST /admin/user/delete
     * 注意：
     * 1. 不能删除管理员账号
     * 2. 会级联删除该用户的检测记录和聊天会话
     *
     * @param req 删除请求（包含 userId）
     * @return 成功提示
     */
    @PostMapping("/user/delete")
    public ApiResult<Void> deleteUser(@Valid @RequestBody DeleteUserRequest req) {
        adminService.deleteUser(req.getUserId());
        return ApiResult.ok(null);
    }

    /**
     * 重置用户密码
     * POST /admin/user/resetPassword
     * 将用户密码重置为默认密码 "123456"
     * 注意：不能重置管理员账号的密码
     *
     * @param req 重置密码请求（包含 userId）
     * @return 成功提示
     */
    @PostMapping("/user/resetPassword")
    public ApiResult<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        adminService.resetPassword(req);
        return ApiResult.ok(null);
    }
}
