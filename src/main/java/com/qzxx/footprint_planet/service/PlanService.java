package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.PlanDTO;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.vo.PageVO;

/** 旅游规划 */
public interface PlanService {
    PageVO<Plan> getPlans(Integer page, Integer pageSize);

    Plan addPlan(PlanDTO planDTO);

    Plan getPlan(Long id);

    Plan updatePlan(Long id, PlanDTO planDTO);

    void deletePlan(Long id);

    Plan copyPlan(Long id);
}
