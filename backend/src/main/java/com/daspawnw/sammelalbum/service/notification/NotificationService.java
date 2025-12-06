package com.daspawnw.sammelalbum.service.notification;

import java.util.List;

public interface NotificationService {
    void sendExchangeNotification(Long offererId, List<String> messages);

    void sendPasswordResetNotification(Long userId, String message);
}
