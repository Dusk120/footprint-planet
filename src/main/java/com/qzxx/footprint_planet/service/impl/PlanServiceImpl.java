package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.common.PlanRole;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.*;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.entity.PlanCollaborator;
import com.qzxx.footprint_planet.entity.PlanItem;
import com.qzxx.footprint_planet.entity.PlanLeg;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.mapper.PlanCollaboratorMapper;
import com.qzxx.footprint_planet.mapper.PlanItemMapper;
import com.qzxx.footprint_planet.mapper.PlanLegMapper;
import com.qzxx.footprint_planet.mapper.PlanMapper;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.service.PlanService;
import com.qzxx.footprint_planet.vo.CollaboratorVO;
import com.qzxx.footprint_planet.vo.PageVO;
import com.qzxx.footprint_planet.vo.PlanDetailVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PlanServiceImpl implements PlanService {
    @Resource
    private PlanMapper planMapper;
    @Resource
    private PlanItemMapper planItemMapper;
    @Resource
    private PlanLegMapper planLegMapper;
    @Resource
    private PlanCollaboratorMapper planCollaboratorMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private SimpMessagingTemplate messagingTemplate;


    @Override
    public PageVO<Plan> getPlans(Integer page, Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        Page<Plan> p = new Page<>(page, pageSize);
        IPage<Plan> result = planMapper.selectMyPlans(p, userId);
        return new PageVO<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PlanDetailVO getPlan(Long id) {
        Long userId = BaseContext.getCurrentId();
        Plan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(404, "计划不存在或无权访问");
        }
        PlanDetailVO planDetailVO = BeanUtil.copyProperties(plan, PlanDetailVO.class);
        // 一次性查出该计划全部协作人
        List<PlanCollaborator> allCollaboratorList = planCollaboratorMapper.selectList(
                new LambdaQueryWrapper<PlanCollaborator>()
                        .eq(PlanCollaborator::getPlanId, id)
        );
        PlanCollaborator myCollaborator = allCollaboratorList.stream()
                .filter(pc -> pc.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        String role=null;
        if(myCollaborator != null){
            role = myCollaborator.getRole();
        }
        if(plan.getUserId().equals(userId)){
            role = PlanRole.OWNER.name().toLowerCase();
        }
        if (role == null && !Boolean.TRUE.equals(plan.getIsPublic())) {
            throw new BusinessException(404, "计划不存在或无权访问");
        }
        else if (role != null) {
            planDetailVO.setMyRole(role);
        }
        else{
            planDetailVO.setMyRole(PlanRole.VIEWER.name().toLowerCase());
        }
        List<PlanItem> planItems = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, id)
                .orderByAsc(PlanItem::getDayNo, PlanItem::getSortNo));
        List<PlanLeg> planLegs = planLegMapper.selectList(new LambdaQueryWrapper<PlanLeg>()
                .eq(PlanLeg::getPlanId, id));
        // 一次性查出所有协作人
        Set<Long> userIds = new HashSet<>();
        userIds.add(plan.getUserId());
        allCollaboratorList.forEach(c -> userIds.add(c.getUserId()));
        List<User> userList = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (k1, k2) -> k1));
        List<CollaboratorVO> collaborators = new ArrayList<>();
        // 组装owner
        User ownerUser = userMap.get(plan.getUserId());
        CollaboratorVO ownerVO = new CollaboratorVO();
        ownerVO.setUserId(plan.getUserId());
        ownerVO.setNickname(ownerUser != null ? ownerUser.getNickname() : "未知用户");
        ownerVO.setAvatar(ownerUser != null ? ownerUser.getAvatar() : User.DEFAULT_AVATAR);
        ownerVO.setRole("owner");
        ownerVO.setJoinedAt(plan.getCreatedAt());
        collaborators.add(ownerVO);
        // 组装协作人
        for (PlanCollaborator c : allCollaboratorList) {
            CollaboratorVO collaboratorVO = new CollaboratorVO();
            User user = userMap.get(c.getUserId());
            collaboratorVO.setAvatar(user != null ? user.getAvatar() : User.DEFAULT_AVATAR);
            collaboratorVO.setNickname(user != null ? user.getNickname() : "未知用户");
            BeanUtil.copyProperties(c, collaboratorVO);
            collaborators.add(collaboratorVO);
        }
        planDetailVO.setItems(planItems);
        planDetailVO.setLegs(planLegs);
        planDetailVO.setCollaborators(collaborators);
        return planDetailVO;
    }

    @Override
    public Plan addPlan(PlanDTO planDTO) {
        Plan plan = new Plan();
        BeanUtil.copyProperties(planDTO, plan);
        plan.setUserId(BaseContext.getCurrentId());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setCollabLocked(false);
        plan.setCollabDeadline(null);
        plan.setVersion(0);
        planMapper.insert(plan);
        return plan;
    }

    @Override
    public Plan updatePlan(Long id, PlanDTO planDTO) {
        Plan plan = requireEditor(id);
        BeanUtil.copyProperties(planDTO, plan,
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setIgnoreProperties("id","userId","version","collabLocked","collabDeadline","createdAt"));
        int rows = planMapper.updateById(plan);
        if (rows <= 0) {
            throw new BusinessException(409, "计划已被他人修改，请刷新后重试");
        }
        broadcastEvent(id, "plan_update");
        return plan;
    }

    @Override
    public void deletePlan(Long id) {
       requireOwner(id);
        planMapper.deleteById(id);
        broadcastEvent(id, "plan_delete");
    }

    @Transactional
    @Override
    public Plan copyPlan(Long id) {
        Long userId = BaseContext.getCurrentId();
        Plan source = planMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(404, "计划不存在");
        }
        if (!Boolean.TRUE.equals(source.getIsPublic()) && getRole(id, userId) == null) {
            throw new BusinessException(404, "计划不存在或无权访问");
        }
        Plan copy = BeanUtil.copyProperties(source, Plan.class,
                "id", "userId", "version", "collabLocked", "collabDeadline", "createdAt");
        copy.setUserId(userId);
        copy.setVersion(0);
        copy.setCollabLocked(false);
        copy.setCollabDeadline(null);
        planMapper.insert(copy);
        // 复制 plan_item，记录「旧 itemId → 新 itemId」
        List<PlanItem> items = planItemMapper.selectList(
                new LambdaQueryWrapper<PlanItem>().eq(PlanItem::getPlanId, id));
        Map<Long, Long> idMap = new HashMap<>();
        for (PlanItem item : items) {
            Long oldId = item.getId();
            item.setId(null);               // 置空 → 走自增，否则主键冲突
            item.setPlanId(copy.getId());
            item.setCreatedAt(LocalDateTime.now());        // 置空 → 数据库 DEFAULT CURRENT_TIMESTAMP 生成新时间
            planItemMapper.insert(item);
            idMap.put(oldId, item.getId()); // 记下旧→新映射
        }
        //  复制 plan_leg，from/to 映射到新 item id，路线原样搬运（不调高德）
        List<PlanLeg> legs = planLegMapper.selectList(
                new LambdaQueryWrapper<PlanLeg>().eq(PlanLeg::getPlanId, id));
        for (PlanLeg leg : legs) {
            leg.setId(null);
            leg.setPlanId(copy.getId());
            leg.setFromItemId(idMap.get(leg.getFromItemId()));
            leg.setToItemId(idMap.get(leg.getToItemId()));
            leg.setCreatedAt(LocalDateTime.now());
            planLegMapper.insert(leg);
        }
        return copy;
    }

    @Override
    public PlanRole getRole(Long planId, Long userId) {
        if (userId == null) {
            return null;
        }
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "规划不存在");
        }
        if (plan.getUserId().equals(userId)) {
            return PlanRole.OWNER;
        }
        PlanCollaborator c = planCollaboratorMapper.selectOne(
                new LambdaQueryWrapper<PlanCollaborator>()
                        .eq(PlanCollaborator::getPlanId, planId)
                        .eq(PlanCollaborator::getUserId, userId));
        if (c == null) {
            return null;
        }
        return "viewer".equals(c.getRole()) ? PlanRole.VIEWER : PlanRole.EDITOR;
    }


    @Override
    public void setCover(Long id, String imageUrl) {
        requireOwner(id);
        Plan plan = planMapper.selectById(id);
        plan.setImage(imageUrl);
        int rows = planMapper.updateById(plan);
        if (rows <= 0) {
            throw new BusinessException(409, "计划已被他人修改，请刷新后重试");
        }
    }

    @Transactional
    @Override
    public PlanItem addItem(Long planId, PlanItemDTO dto) {
        Plan plan = requireEditor(planId);
        // 忽略前端传的 sortNo，改为追加到该天末尾：dayNo 为空默认第 1 天，
        // 新条目 sortNo = 该天现有条目数（0、1、2… 依次追加）
        Integer dayNo = dto.getDayNo() == null ? 1 : dto.getDayNo();
        Long count = planItemMapper.selectCount(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .eq(PlanItem::getDayNo, dayNo));
        PlanItem planItem = new PlanItem();
        BeanUtil.copyProperties(dto, planItem);
        planItem.setPlanId(planId);
        planItem.setDayNo(dayNo);
        planItem.setSortNo(count.intValue());
        planItem.setCreatedAt(LocalDateTime.now());
        planItemMapper.insert(planItem);
        int rows = planMapper.updateById(plan);
        if (rows <= 0) {
            throw new BusinessException(409, "计划已被他人修改，请刷新后重试");
        }
        broadcastEvent(planId, "item_add", "itemId", planItem.getId(), "itemName", planItem.getName());
        return planItem;
    }

    @Transactional
    @Override
    public PlanItem updateItem(Long planId, Long itemId, PlanItemDTO dto) {
        Plan plan = requireEditor(planId);
        PlanItem planItem = planItemMapper.selectById(itemId);
        if(planItem == null){
            throw new BusinessException(404, "计划项不存在");
        }
        if(planItem.getPlanId().equals(planId)){
            BeanUtil.copyProperties(dto, planItem,
                    CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setIgnoreProperties("id","planId","createdAt"));
            planItemMapper.updateById(planItem);
            int rows = planMapper.updateById(plan);
            if (rows <= 0) {
                throw new BusinessException(409, "计划已被他人修改，请刷新后重试");
            }
            broadcastEvent(planId, "item_update", "itemId", itemId, "itemName", planItem.getName());
            return planItem;
        }
        else{
            throw new BusinessException(404, "条目不存在或不匹配");
        }
    }

    @Transactional
    @Override
    public void deleteItem(Long planId, Long itemId) {
        Plan plan = requireEditor(planId);
        // 删除前先取名称，供动态流展示「谁删除了哪个景点」
        PlanItem target = planItemMapper.selectById(itemId);
        if (target == null || !target.getPlanId().equals(planId)) {
            throw new BusinessException(404, "条目不存在或不匹配");
        }
        String itemName = target.getName();
        planItemMapper.deleteById(itemId);
        int rows = planMapper.updateById(plan);
        if (rows <= 0) {
            throw new BusinessException(409, "计划已被他人修改，请刷新后重试");
        }
        broadcastEvent(planId, "item_delete", "itemId", itemId, "itemName", itemName);
    }

    @Transactional
    @Override
    public void reorderItems(Long planId, ReorderDTO dto) {
       requireEditor(planId);
        int cas = planMapper.update(null, new LambdaUpdateWrapper<Plan>()
                .eq(Plan::getId, planId)
                .eq(Plan::getVersion, dto.getVersion())
                .set(Plan::getVersion, dto.getVersion() + 1));
        if (cas == 0) {
            throw new BusinessException(409, "计划已被他人修改，请刷新后重试");
        }
        List<PlanItem> dayItemList = planItemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId));
        Map<Long, PlanItem> itemMap = dayItemList.stream()
                .collect(Collectors.toMap(PlanItem::getId, p -> p, (k1, k2) -> k1));

        // 遍历所有天，逐条 set dayNo + sortNo
        for (DayOrderDTO day : dto.getDays()) {
            for (int i = 0; i < day.getItemIds().size(); i++) {
                PlanItem item = itemMap.get(day.getItemIds().get(i));
                if (item == null) throw new BusinessException(400, "计划项不存在");
                item.setDayNo(day.getDayNo());
                item.setSortNo(i);
                planItemMapper.updateById(item);
            }
        }
        //
        // 未出现在任何 itemIds 里的条目 → dayNo 置 null（防御，正常前端传全量不会触发）
        Set<Long> mentioned = dto.getDays().stream()
                .flatMap(d -> d.getItemIds().stream())
                .collect(Collectors.toSet());
        for (PlanItem item : dayItemList) {
            if (!mentioned.contains(item.getId())) {
                item.setDayNo(null);
                item.setSortNo(null);
                planItemMapper.updateById(item);
            }
        }
        planLegMapper.delete(new LambdaQueryWrapper<PlanLeg>()
                .eq(PlanLeg::getPlanId, planId));
        broadcastEvent(planId, "reorder");
    }

    @Transactional
    @Override
    public PlanLeg saveLeg(Long planId, PlanLegDTO dto) {
        Plan plan = requireEditor(planId);
        // 覆盖式保存：一个起点（from_item_id）只对应一条交通段。
        // 先删掉同起点的旧 leg 再插入，避免「修改出行方案」时堆积重复记录
        planLegMapper.delete(new LambdaQueryWrapper<PlanLeg>()
                .eq(PlanLeg::getPlanId, planId)
                .eq(PlanLeg::getFromItemId, dto.getFromItemId()));
        PlanLeg planLeg = new PlanLeg();
        BeanUtil.copyProperties(dto, planLeg);
        planLeg.setPlanId(planId);
        planLegMapper.insert(planLeg);
        int rows = planMapper.updateById(plan);
        if (rows <= 0) {
            throw new BusinessException(409, "计划已被他人修改，请刷新后重试");
        }
        broadcastEvent(planId, "leg_save");
        return planLeg;
    }

    @Override
    public void setCollabLocked(Long id, Boolean locked) {
        Plan plan = requireOwner(id);
        plan.setCollabLocked(locked);
        planMapper.updateById(plan);
        broadcastEvent(id, "lock", "locked", locked, "reason", "owner");
    }

    @Override
    public void setCollabDeadline(Long id, LocalDateTime deadline) {
        Plan plan = requireOwner(id);
        plan.setCollabDeadline(deadline);
        planMapper.updateById(plan);
        broadcastEvent(id, "deadline", "deadline", deadline);
    }

    @Override
    public void removeCollaborator(Long planId, Long userId) {
        requireOwner(planId);
        if(userId.equals(BaseContext.getCurrentId())){
            throw new BusinessException(400, "不能移除自己");
        }
        PlanCollaborator collaborator = planCollaboratorMapper.selectOne(new LambdaQueryWrapper<PlanCollaborator>()
                .eq(PlanCollaborator::getPlanId, planId)
                .eq(PlanCollaborator::getUserId, userId));
        if (collaborator == null) {
            throw new BusinessException(404, "协作人不存在");
        }
        planCollaboratorMapper.deleteById(collaborator);
        broadcastEvent(planId, "collaborator_remove", "userId", userId);
    }

    @Override
    public Plan requireEditor(Long planId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "规划不存在");
        }
        PlanRole role = getRole(planId, BaseContext.getCurrentId());
        if (role != PlanRole.OWNER && role != PlanRole.EDITOR) {
            throw new BusinessException(403, "无权限操作");
        }
        if (role == PlanRole.EDITOR && isCollabLocked(plan)) {
            throw new BusinessException(403, "计划已锁定或已到截止时间，暂不可编辑");
        }
        return plan;
}

    @Override
    public Plan requireOwner(Long planId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "规划不存在");
        }
        PlanRole role = getRole(planId, BaseContext.getCurrentId());
        if (role != PlanRole.OWNER) {
            throw new BusinessException(403, "无权限操作");
        }
        return plan;
    }
    private boolean isCollabLocked(Plan plan) {
        return Boolean.TRUE.equals(plan.getCollabLocked())
                || (plan.getCollabDeadline() != null
                && LocalDateTime.now().isAfter(plan.getCollabDeadline()));
    }

    /** 广播计划变更事件给所有订阅者（kv 成对传入业务字段，如 "itemId", 3L） */
    @Override
    public void broadcastEvent(Long planId, String type, Object... kv) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("planId", planId);
        Long actorId = BaseContext.getCurrentId();
        event.put("actorId", actorId);
        // 附带操作者昵称，供前端「动态流」直接展示「谁做了什么」
        User actor = actorId != null ? userMapper.selectById(actorId) : null;
        event.put("actorName", actor != null ? actor.getNickname() : "未知用户");
        event.put("timestamp", LocalDateTime.now().toString());
        for (int i = 0; i < kv.length; i += 2) {
            event.put((String) kv[i], kv[i + 1]);
        }
        try {
            messagingTemplate.convertAndSend("/topic/plan/" + planId, (Object) event);
        } catch (Exception e) {
            // 实时广播是「尽力而为」的同步：数据已落库，推送失败不影响主流程，订阅者刷新仍可见
            log.warn("计划变更广播推送失败，已忽略: planId={}, type={}", planId, type, e);
        }
    }
}
