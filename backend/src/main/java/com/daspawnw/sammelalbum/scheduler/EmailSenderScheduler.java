package com.daspawnw.sammelalbum.scheduler;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import com.daspawnw.sammelalbum.repository.EmailOutboxRepository;
import com.daspawnw.sammelalbum.service.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailSenderScheduler {

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailSender emailSender;

    @Scheduled(cron = "0 * * * * *") // Every minute
    @SchedulerLock(name = "EmailSenderScheduler_processPendingEmails", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    public void processPendingEmails() {
        processPendingEmailsInternal();
    }

    @Transactional
    public void processPendingEmailsInternal() {
        log.debug("Checking for pending emails...");
        List<EmailOutbox> pendingEmails = emailOutboxRepository.findBatchToProcess(EmailStatus.PENDING,
                LocalDateTime.now());

        if (pendingEmails.isEmpty()) {
            log.debug("No pending emails found.");
            return;
        }

        log.info("Found {} pending emails. Processing...", pendingEmails.size());

        for (EmailOutbox email : pendingEmails) {
            try {
                emailSender.send(email);
                email.setStatus(EmailStatus.SENT);
                email.setSentAt(LocalDateTime.now());
                log.info("Email {} sent successfully to {}", email.getId(), email.getRecipientEmail());
            } catch (Exception e) {
                log.error("Failed to send email {}", email.getId(), e);
                handleFailure(email, e);
            }
        }

        emailOutboxRepository.saveAll(pendingEmails);
    }

    private void handleFailure(EmailOutbox email, Exception e) {
        int newRetryCount = email.getRetryCount() + 1;
        email.setRetryCount(newRetryCount);
        email.setErrorMessage(e.getMessage());

        if (newRetryCount >= 3) {
            email.setStatus(EmailStatus.FAILED);
            log.warn("Email {} failed after {} attempts. Marking as FAILED.", email.getId(), newRetryCount);
        } else {
            // Exponential backoff: 2^retryCount * 1 minute (e.g., 2m, 4m, etc.)
            // Or simpler: retryCount * 1 minute?
            // User asked for exponential backoff.
            // Let's say base delay is 1 minute.
            // Retry 1: 2^1 = 2 minutes
            // Retry 2: 2^2 = 4 minutes
            long delayMinutes = (long) Math.pow(2, newRetryCount);
            LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(delayMinutes);
            email.setNextRetryAt(nextRetry);
            log.info("Email {} scheduled for retry #{} at {}", email.getId(), newRetryCount, nextRetry);
        }
    }
}
