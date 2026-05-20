package com.rice.rice.controller;

import com.rice.rice.common.ApiResult;
import com.rice.rice.dto.LoginRequest;
import com.rice.rice.dto.RegisterRequest;
import com.rice.rice.dto.UpdateUserRequest;
import com.rice.rice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户控制器
 * 提供用户注册、登录、信息查询和更新等 API 接口
 * 所有接口路径前缀：/user/user
 */
@RestController
@RequestMapping("/user/user")
public class UserController {

    private final UserService userService;  // 用户服务层

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录接口
     * POST /user/user/login
     *
     * @param req 登录请求（用户名、密码）
     * @return JWT Token
     */
    @PostMapping("/login")
    public ApiResult<String> login(@Valid @RequestBody LoginRequest req) {
        return ApiResult.ok(userService.login(req));
    }

    /**
     * 用户注册接口
     * POST /user/user/register
     *
     * @param req 注册请求（用户名、邮箱、密码）
     * @return 成功提示
     */
    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest req) {
        userService.register(req);
        return ApiResult.ok(null);
    }

    /**
     * 获取当前用户信息接口
     * GET /user/user/getUserInfo
     * 需要 JWT Token 认证
     *
     * @return 用户详细信息
     */
    @GetMapping("/getUserInfo")
    public ApiResult<Map<String, Object>> getUserInfo() {
        return ApiResult.ok(userService.getUserInfo());
    }

    /**
     * 更新用户信息接口
     * POST /user/user/updateUser
     * 需要 JWT Token 认证，只能修改自己的信息
     *
     * @param req 更新请求（包含要修改的字段）
     * @return 更新后的用户信息
     */
    @PostMapping("/updateUser")
    public ApiResult<Map<String, Object>> updateUser(@Valid @RequestBody UpdateUserRequest req) {
        return ApiResult.ok(userService.updateUser(req));
    }

    /**
     * 用户登出接口
     * POST /user/user/logout
     * 注意：JWT 是无状态的，客户端只需删除本地 Token 即可
     *
     * @return 成功提示
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.ok(null);
    }
}
