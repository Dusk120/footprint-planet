package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.PlanDTO;
import com.qzxx.footprint_planet.entity.Plan;

import java.util.List;

/** 旅游规划 */
public interface PlanService {
    List<Plan> getPlans();

    Plan addPlan(PlanDTO planDTO);

    Plan getPlan(Long id);

    Plan updatePlan(Long id, PlanDTO planDTO);

    void deletePlan(Long id);

    Plan copyPlan(Long id);
}
