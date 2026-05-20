package com.rice.rice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 分页查询请求 DTO
 * 用于所有需要分页的接口（如用户列表、检测记录列表等）
 */
public class PageQuery {

    /** 页码（从 1 开始，必填，最小值为 1） */
    @NotNull
    @Min(1)
    private Integer page;

    /** 每页大小（必填，最小值为 1） */
    @NotNull
    @Min(1)
    private Integer pageSize;

    /** 搜索关键词（可选，用于模糊查询） */
    private String keyword;

    /**
     * 获取页码
     *
     * @return 页码（从 1 开始）
     */
    public Integer getPage() {
        return page;
    }

    /**
     * 设置页码
     *
     * @param page 页码（从 1 开始）
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * 获取每页大小
     *
     * @return 每页记录数
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页大小
     *
     * @param pageSize 每页记录数
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 获取搜索关键词
     *
     * @return 关键词，可能为 null
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 设置搜索关键词
     *
     * @param keyword 搜索关键词
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
