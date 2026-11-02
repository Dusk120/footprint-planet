package com.qzxx.footprint_planet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qzxx.footprint_planet.entity.PlanInvite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PlanInviteMapper extends BaseMapper<PlanInvite> {

    /**
     * 原子占坑：在「未满额」的前提下把已用次数 +1（检查与自增合一，杜绝并发超限）。
     * max_uses = 0 表示不限；返回 0 表示已满额或邀请不存在。
     */
    @Update("UPDATE plan_invite SET used_count = used_count + 1 " +
            "WHERE id = #{id} AND (max_uses = 0 OR used_count < max_uses)")
    int incrementUsedIfAvailable(@Param("id") Long id);
}
