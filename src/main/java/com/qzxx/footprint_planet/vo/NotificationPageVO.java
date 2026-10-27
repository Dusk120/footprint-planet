package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 通知列表分页响应：在分页基础上附带未读总数（铃铛红点用） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageVO {

    private List<NotificationVO> list;

    private long total;

    private long page;

    private long pageSize;

    private long unreadCount;
}
