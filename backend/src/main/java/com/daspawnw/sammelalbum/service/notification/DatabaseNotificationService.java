package com.daspawnw.sammelalbum.service.notification;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.EmailOutboxRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseNotificationService implements NotificationService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void sendExchangeNotification(Long offererId, List<String> messages) {
        log.info("Persisting exchange notification for Offerer ID: {}", offererId);

        String recipientEmail = userRepository.findById(offererId)
                .map(User::getMail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for ID: " + offererId));

        String body = String.join("\n", messages);

        EmailOutbox email = EmailOutbox.builder()
                .recipientEmail(recipientEmail)
                .subject("Neue Tauschanfragen")
                .body(body)
                .status(EmailStatus.PENDING)
                .build();

        emailOutboxRepository.save(email);
        log.info("Exchange notification persisted with ID: {}", email.getId());
    }

    @Override
    @Transactional
    public void sendPasswordResetNotification(Long userId, String message) {
        log.info("Persisting password reset notification for User ID: {}", userId);

        String recipientEmail = userRepository.findById(userId)
                .map(User::getMail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for ID: " + userId));

        EmailOutbox email = EmailOutbox.builder()
                .recipientEmail(recipientEmail)
                .subject("Passwort zurücksetzen / Password Reset")
                .body(message)
                .status(EmailStatus.PENDING)
                .build();

        emailOutboxRepository.save(email);
        log.info("Password reset notification persisted with ID: {}", email.getId());
    }
}
