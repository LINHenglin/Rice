package com.rice.rice.dto;

import java.util.List;

/**
 * 分页数据响应 DTO（泛型）
 * 用于封装分页查询的结果
 *
 * @param <T> 数据类型（如 User、DetectionRecord 等）
 */
public class PagedData<T> {

    /** 当前页的数据列表 */
    private List<T> records;
    
    /** 总记录数 */
    private long total;

    /** 默认构造函数 */
    public PagedData() {
    }

    /**
     * 构造函数
     *
     * @param records 当前页的数据列表
     * @param total   总记录数
     */
    public PagedData(List<T> records, long total) {
        this.records = records;
        this.total = total;
    }

    /**
     * 获取当前页的数据列表
     *
     * @return 数据列表
     */
    public List<T> getRecords() {
        return records;
    }

    /**
     * 设置当前页的数据列表
     *
     * @param records 数据列表
     */
    public void setRecords(List<T> records) {
        this.records = records;
    }

    /**
     * 获取总记录数
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置总记录数
     *
     * @param total 总记录数
     */
    public void setTotal(long total) {
        this.total = total;
    }
}
