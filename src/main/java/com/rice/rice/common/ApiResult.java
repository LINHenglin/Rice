package com.rice.rice.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应结果封装
 * 所有接口返回此格式，保证前后端交互一致性
 * 
 * 响应格式示例：
 * {
 *   "code": "200",
 *   "message": "ok",
 *   "data": {...},
 *   "success": true
 * }
 *
 * @param <T> 响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)  // 序列化时忽略 null 字段
public class ApiResult<T> {

    private String code;      // 状态码（200=成功，500=失败）
    private String message;   // 响应消息
    private T data;           // 响应数据
    private Boolean success;  // 是否成功

    /**
     * 创建成功响应（默认消息 "ok"）
     *
     * @param data 响应数据
     * @return ApiResult 对象
     */
    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode("200");
        r.setMessage("ok");
        r.setData(data);
        r.setSuccess(true);
        return r;
    }

    /**
     * 创建成功响应（自定义消息）
     *
     * @param message 响应消息
     * @param data    响应数据
     */
    public static <T> ApiResult<T> ok(String message, T data) {
        ApiResult<T> r = ok(data);
        r.setMessage(message);
        return r;
    }

    /**
     创建失败响应
     @param message 错误消息
     */
    public static <T> ApiResult<T> fail(String message) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode("500");
        r.setMessage(message);
        r.setSuccess(false);
        return r;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
