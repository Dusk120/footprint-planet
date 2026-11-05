package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.config.MybatisPlusConfig;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.entity.Notification;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.mapper.NotificationsMapper;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.service.NotificationsService;
import com.qzxx.footprint_planet.vo.NotificationPageVO;
import com.qzxx.footprint_planet.vo.NotificationVO;
import jakarta.annotation.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationsServiceImpl implements NotificationsService {

    /** 推送目标：相对 /user 前缀，前端订阅 /user/queue/notifications */
    private static final String DEST = "/queue/notifications";

    @Resource
    private NotificationsMapper notificationsMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationPageVO getNotifications(String type, Long cursor, Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        List<String> types = typesOf(type);
        // 钳制到 [1, MAX_PAGE_SIZE]：下面要「多取一条」判断 hasMore，
        // 超过上限时多取的那条会被分页拦截器截掉，hasMore 恒为 false，加载会提前终止
        pageSize = Math.min(Math.max(pageSize == null ? 10 : pageSize, 1),
                MybatisPlusConfig.MAX_PAGE_SIZE);
        Page<Notification> pageData = new Page<>(1, pageSize+1);
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .in(Notification::getType, types)
                .orderByDesc(Notification::getId);
        if (cursor != null) {
            qw.lt(Notification::getId, cursor);
        }
        Page<Notification> resultPage = notificationsMapper.selectPage(pageData, qw);
        List<NotificationVO> list = resultPage.getRecords().stream().map(record -> {
            NotificationVO notificationVO = new NotificationVO();
            BeanUtil.copyProperties(record, notificationVO);
            return notificationVO;
        }).toList();
        boolean hasMore = list.size() > pageSize;
        if (hasMore){
            list = list.subList(0, pageSize);
        }
        Long nextCursor = hasMore ? list.get(list.size() - 1).getId() : null;
        long unreadCount = notificationsMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .in(Notification::getType, types)
                        .eq(Notification::getIsRead, 0));
        long totalUnreadCount = notificationsMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        return new NotificationPageVO(list, nextCursor, hasMore, unreadCount, totalUnreadCount);
    }
    private List<String> typesOf(String type) {
        if ("like".equals(type)) return List.of("like_note", "like_comment");
        if ("comment".equals(type)) return List.of("comment", "reply");
        throw new BusinessException(400, "不支持的 type: " + type);
    }

    @Override
    public Void readNotification(Long id) {
        Notification notification = notificationsMapper.selectById(id);
        if (notification != null && notification.getUserId().equals(BaseContext.getCurrentId())) {
            notification.setIsRead(1);
            notificationsMapper.updateById(notification);
            return null;
        }
        throw new BusinessException(404, "通知不存在 / 不属于当前用户");
    }

    @Override
    public Void readAllNotifications(String type) {
        Long userId = BaseContext.getCurrentId();
        notificationsMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .in(Notification::getType, typesOf(type))
                .set(Notification::getIsRead, 1));
        return null;
    }

    @Override
    public void notify(Long receiverId, String type, Long noteId, String noteTitle, String content) {
        Long senderId = BaseContext.getCurrentId();
        // 不通知自己；接收者为空也跳过
        if (receiverId == null || receiverId.equals(senderId)) {
            return;
        }
        User sender = userMapper.selectById(senderId);
        Notification n = new Notification();
        n.setUserId(receiverId);
        n.setType(type);
        n.setSenderId(senderId);
        n.setSenderNickname(sender != null ? sender.getNickname() : null);
        n.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        n.setNoteId(noteId);
        n.setNoteTitle(noteTitle);
        n.setContent(content);
        n.setIsRead(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationsMapper.insert(n);

        // 落库成功后实时推送（尽力而为，失败不影响主流程；已落库，用户下次拉列表仍能看到）
        NotificationVO vo = new NotificationVO();
        BeanUtil.copyProperties(n, vo);
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(receiverId), DEST, vo);
        } catch (Exception e) {
            // 推送失败忽略
        }
    }
}
