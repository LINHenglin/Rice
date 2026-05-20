package com.rice.rice.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化工具类
 * 提供 LocalDateTime 与字符串之间的格式化转换
 * 使用 ISO 8601 标准格式（如：2024-01-01T12:00:00）
 * 所有方法都是静态方法，无需实例化
 */
public final class TimeFormat {

    /** ISO 8601 本地日期时间格式化器（yyyy-MM-dd'T'HH:mm:ss） */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 私有构造函数，防止实例化 */
    private TimeFormat() {
    }

    /**
     * 将 LocalDateTime 格式化为 ISO 8601 字符串
     * 如果输入为 null，则返回 null
     *
     * @param t 日期时间对象
     * @return 格式化后的字符串（如 "2024-01-01T12:00:00"），如果输入为 null 则返回 null
     */
    public static String iso(LocalDateTime t) {
        return t == null ? null : t.format(ISO);
    }
}
