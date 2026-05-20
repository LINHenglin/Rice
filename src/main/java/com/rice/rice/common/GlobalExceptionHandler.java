package com.rice.rice.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一捕获和处理所有 Controller 层抛出的异常
 * 返回统一的 ApiResult 格式，保证前后端交互一致性
 * 
 * 使用 @RestControllerAdvice 注解，自动应用到所有 @RestController
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（BusinessException）
     * 这是最常见的异常类型，由业务逻辑主动抛出
     * 
     * HTTP 状态码：200 OK（业务失败但请求成功）
     *
     * @param e 业务异常对象
     * @return 失败响应（包含错误消息）
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleBusiness(BusinessException e) {
        return ApiResult.fail(e.getMessage());
    }

    /**
     * 处理权限拒绝异常（AccessDeniedException）
     * 当用户没有访问某个接口的权限时抛出
     * 
     * HTTP 状态码：403 Forbidden
     *
     * @param e 权限拒绝异常
     * @return 失败响应（"无权限"）
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleAccessDenied(AccessDeniedException e) {
        return ApiResult.fail("无权限");
    }

    /**
     * 处理认证异常（AuthenticationException）
     * 包括用户名密码错误、账号被禁用等认证相关问题
     * 
     * HTTP 状态码：401 Unauthorized
     *
     * @param e 认证异常对象
     * @return 失败响应（根据具体异常类型返回不同消息）
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleAuthentication(AuthenticationException e) {
        // 用户名或密码错误
        if (e instanceof BadCredentialsException) {
            return ApiResult.fail("用户名或密码错误");
        }
        // 账号已被禁用
        if (e instanceof DisabledException) {
            return ApiResult.fail("账号已被禁用");
        }
        // 其他认证失败
        return ApiResult.fail(e.getMessage() != null ? e.getMessage() : "认证失败");
    }

    /**
     * 处理数据完整性违例异常（DataIntegrityViolationException）
     * 通常由数据库约束冲突引起（如唯一键冲突、外键约束等）
     * 
     * HTTP 状态码：200 OK
     *
     * @param e 数据完整性异常
     * @return 失败响应（"数据冲突，请稍后重试"）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        return ApiResult.fail("数据冲突，请稍后重试");
    }

    /**
     * 处理参数验证异常（MethodArgumentNotValidException）
     * 当 @Valid 注解的参数验证失败时抛出
     * 
     * HTTP 状态码：400 Bad Request
     *
     * @param e 参数验证异常
     * @return 失败响应（包含第一个验证失败的字段错误消息）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        // 获取第一个验证失败的字段
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数错误";
        return ApiResult.fail(msg);
    }

    /**
     * 处理其他未预见的异常（兜底 handler）
     * 捕获所有未被上述 handler 处理的 Exception
     * 
     * HTTP 状态码：500 Internal Server Error
     *
     * @param e 异常对象
     * @return 失败响应（包含错误消息或"服务器错误"）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleOther(Exception e) {
        return ApiResult.fail(e.getMessage() != null ? e.getMessage() : "服务器错误");
    }
}
