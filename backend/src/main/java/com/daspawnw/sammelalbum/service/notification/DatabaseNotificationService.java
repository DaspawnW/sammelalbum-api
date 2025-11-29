package com.daspawnw.sammelalbum.service.notification;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import com.daspawnw.sammelalbum.repository.EmailOutboxRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
        log.info("Persisting notification for Offerer ID: {}", offererId);

        String recipientEmail = userRepository.findById(offererId)
                .map(com.daspawnw.sammelalbum.model.User::getMail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for ID: " + offererId));

        String body = String.join("\n", messages);

        EmailOutbox email = EmailOutbox.builder()
                .recipientEmail(recipientEmail)
                .subject("Neue Tauschanfragen")
                .body(body)
                .status(EmailStatus.PENDING)
                .build();

        emailOutboxRepository.save(email);
        log.info("Notification persisted with ID: {}", email.getId());
    }
}
