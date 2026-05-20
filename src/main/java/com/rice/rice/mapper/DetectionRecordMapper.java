package com.rice.rice.mapper;

import com.rice.rice.entity.DetectionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 检测记录数据访问层（Mapper）
 * 基于 Spring Data JPA，继承 JpaRepository 接口
 * 提供水稻病害检测记录相关的数据库操作方法
 */
public interface DetectionRecordMapper extends JpaRepository<DetectionRecord, Long> {

    /**
     * 分页查询指定用户的检测记录（按创建时间降序排列）
     * 方法名规则：findBy + UserId + OrderBy + CreateTime + Desc
     *
     * @param userId   用户 ID
     * @param pageable 分页参数
     * @return 分页检测记录列表
     */
    Page<DetectionRecord> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 查询指定用户的所有检测记录（不分页）
     * 用于删除用户时收集需要删除的图片 URL
     *
     * @param userId 用户 ID
     * @return 检测记录列表
     */
    java.util.List<DetectionRecord> findByUserId(Long userId);

    /**
     * 统计指定时间之后的检测记录数量
     * JPQL 查询：SELECT COUNT(d) FROM DetectionRecord d WHERE d.createTime >= :start
     *
     * @param start 起始时间
     * @return 检测记录数量
     */
    @Query("SELECT COUNT(d) FROM DetectionRecord d WHERE d.createTime >= :start")
    long countSince(@Param("start") LocalDateTime start);

    /**
     * 删除指定用户的所有检测记录
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);
}
