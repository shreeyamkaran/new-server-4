package com.beehyv.server.service;

import com.beehyv.server.dto.NotificationDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface NotificationService {

    SseEmitter connect(String token, Authentication authentication);

    List<NotificationDto> fetchAllNotifications(Long receiverId);

    void dismissNotification(Long notificationId);

    void remindManager(Long taskId);
}
