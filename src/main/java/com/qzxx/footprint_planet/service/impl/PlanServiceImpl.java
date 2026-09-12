package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.PlanDTO;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.mapper.PlanMapper;
import com.qzxx.footprint_planet.service.PlanService;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class PlanServiceImpl implements PlanService {
    @Resource
    private PlanMapper planMapper;

    @Override
    public PageVO<Plan> getPlans(Integer page, Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        Page<Plan> p = new Page<>(page, pageSize);
        p.addOrder(OrderItem.desc("created_at"));
        Page<Plan> result = planMapper.selectPage(p,
                new LambdaQueryWrapper<Plan>().eq(Plan::getUserId, userId));
        return new PageVO<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public Plan addPlan(PlanDTO planDTO) {
        if(planDTO.getIsPublic() == null){
            planDTO.setIsPublic(true);
        }
        Plan plan = new Plan();
        BeanUtil.copyProperties(planDTO, plan);
        plan.setUserId(BaseContext.getCurrentId());
        plan.setCreatedAt(java.time.LocalDateTime.now());
        planMapper.insert(plan);
        return plan;
    }

    @Override
    public Plan getPlan(Long id) {
        Plan plan = planMapper.selectById(id);
        if (plan == null ) {
            throw new BusinessException(404,"规划不存在");
        }
        if (plan.getUserId().equals(BaseContext.getCurrentId()) ) {
            return plan;
        }
        if (plan.getIsPublic() ) {
            return plan;
        }
        throw new BusinessException(404,"该规划不公开");
    }

    @Override
    public Plan updatePlan(Long id, PlanDTO planDTO) {
        Plan plan = planMapper.selectById(id);
        if (plan == null ) {
            throw new BusinessException(404,"规划不存在");
        }
        if (!plan.getUserId().equals(BaseContext.getCurrentId()) ) {
            throw new BusinessException(403,"无权限修改");
        }
        BeanUtil.copyProperties(planDTO, plan);
        planMapper.updateById(plan);
        return plan;
    }

    @Override
    public void deletePlan(Long id) {
        Plan plan = planMapper.selectById(id);
        if (plan == null ) {
            throw new BusinessException(404,"规划不存在");
        }
        if (!plan.getUserId().equals(BaseContext.getCurrentId()) ) {
            throw new BusinessException(403,"无权限删除");
        }
        planMapper.deleteById(id);
    }

    @Override
    public Plan copyPlan(Long id) {
        Plan source = planMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(404, "规划不存在");
        }
        // 仅公开规划可被复制；私有（含他人或未登录）一律 404，不暴露其存在
        if (!source.getIsPublic()) {
            throw new BusinessException(404, "该规划不公开");
        }
        Plan copy = new Plan();
        BeanUtil.copyProperties(source, copy);
        copy.setId(null);                       // 重新自增
        copy.setUserId(BaseContext.getCurrentId()); // 归属写当前登录用户
        copy.setIsPublic(true);                // 副本不延续源公开状态
        copy.setCreatedAt(java.time.LocalDateTime.now());
        planMapper.insert(copy);
        return copy;
    }
}
