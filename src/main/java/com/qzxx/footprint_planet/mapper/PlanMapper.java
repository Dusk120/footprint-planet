package com.qzxx.footprint_planet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qzxx.footprint_planet.entity.Plan;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PlanMapper extends BaseMapper<Plan> {

    /**
     * 查询「我创建的 + 我协作参与的」计划（合并去重 + 分页）。
     * collaborated 标记是否以协作人身份参与（pc.user_id 命中则为 1）。
     * 依赖 plan_collaborator 对 (plan_id, user_id) 业务唯一，保证 LEFT JOIN 不放大行数。
     */
    @Select("SELECT p.*, CASE WHEN pc.user_id IS NULL THEN 0 ELSE 1 END AS collaborated " +
            "FROM plan p " +
            "LEFT JOIN plan_collaborator pc ON pc.plan_id = p.id AND pc.user_id = #{userId} " +
            "WHERE p.user_id = #{userId} OR pc.user_id = #{userId} " +
            "ORDER BY p.created_at DESC")
    IPage<Plan> selectMyPlans(IPage<Plan> page, @Param("userId") Long userId);
}
