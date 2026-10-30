package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.common.PlanRole;
import com.qzxx.footprint_planet.dto.request.PlanDTO;
import com.qzxx.footprint_planet.dto.request.PlanItemDTO;
import com.qzxx.footprint_planet.dto.request.PlanLegDTO;
import com.qzxx.footprint_planet.dto.request.ReorderDTO;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.entity.PlanItem;
import com.qzxx.footprint_planet.entity.PlanLeg;
import com.qzxx.footprint_planet.vo.PageVO;
import com.qzxx.footprint_planet.vo.PlanDetailVO;

import java.time.LocalDateTime;

/** 旅游规划（地图驱动行程 + 多人协同） */
public interface PlanService {

    PageVO<Plan> getPlans(Integer page, Integer pageSize);

    /** 规划详情：基本信息 + 行程条目 + 交通段 + 协作者 + 当前用户角色 */
    PlanDetailVO getPlan(Long id);

    Plan addPlan(PlanDTO planDTO);

    Plan updatePlan(Long id, PlanDTO planDTO);

    void deletePlan(Long id);

    Plan copyPlan(Long id);

    /** 查询用户在某计划中的角色；非成员返回 null（owner 由 plan.user_id 判定） */
    PlanRole getRole(Long planId, Long userId);

    /** 校验编辑权限（OWNER/EDITOR），返回 Plan；viewer/非成员抛 403，不存在抛 404 */
    Plan requireEditor(Long planId);

    /** 校验所有者权限（OWNER），返回 Plan；非 owner 抛 403，不存在抛 404 */
    Plan requireOwner(Long planId);

    /** 设置封面（仅 owner） */
    void setCover(Long id, String imageUrl);

    PlanItem addItem(Long planId, PlanItemDTO dto);

    PlanItem updateItem(Long planId, Long itemId, PlanItemDTO dto);

    void deleteItem(Long planId, Long itemId);

    /** DAY 内批量排序：单事务更新 sort_no + plan.version+1 */
    void reorderItems(Long planId, ReorderDTO dto);

    PlanLeg saveLeg(Long planId, PlanLegDTO dto);

    /** 协作锁定 / 解锁（仅 owner） */
    void setCollabLocked(Long id, Boolean locked);

    /** 设置 / 取消创作截止时间（仅 owner，deadline 为 null 表示取消） */
    void setCollabDeadline(Long id, LocalDateTime deadline);

    /** 移除协作者成员（仅 owner） */
    void removeCollaborator(Long planId, Long userId);

    /** 广播计划事件给所有订阅者（type + kv 成对业务字段），供协作模块复用 */
    void broadcastEvent(Long planId, String type, Object... kv);
}
