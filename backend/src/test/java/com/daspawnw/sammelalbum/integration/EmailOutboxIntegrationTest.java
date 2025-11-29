package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import com.daspawnw.sammelalbum.repository.EmailOutboxRepository;
import com.daspawnw.sammelalbum.scheduler.EmailSenderScheduler;
import com.daspawnw.sammelalbum.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/match_scenarios.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class EmailOutboxIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private EmailSenderScheduler emailSenderScheduler;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.springframework.test.context.transaction.BeforeTransaction
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM shedlock");
        jdbcTemplate.execute("DELETE FROM email_outbox");
        jdbcTemplate.execute("DELETE FROM exchange_requests");
        jdbcTemplate.execute("DELETE FROM card_searches");
        jdbcTemplate.execute("DELETE FROM card_offers");
        jdbcTemplate.execute("DELETE FROM credentials");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM stickers");
    }

    @Test
    void shouldPersistAndSendEmail() {
        // 1. Send Notification (should persist to DB)
        Long offererId = 1L; // Main User from match_scenarios.sql
        List<String> messages = List.of("Message 1", "Message 2");

        notificationService.sendExchangeNotification(offererId, messages);
        emailOutboxRepository.flush();

        // 2. Verify it's in DB as PENDING
        List<EmailOutbox> pendingEmails = emailOutboxRepository.findByStatus(EmailStatus.PENDING);
        assertEquals(1, pendingEmails.size());
        EmailOutbox email = pendingEmails.get(0);
        assertEquals("main@example.com", email.getRecipientEmail());
        assertEquals("Neue Tauschanfragen", email.getSubject());
        assertTrue(email.getBody().contains("Message 1"));
        assertTrue(email.getBody().contains("Message 2"));

        // 3. Run Scheduler
        emailSenderScheduler.processPendingEmails();

        // 4. Verify it's now SENT
        List<EmailOutbox> sentEmails = emailOutboxRepository.findByStatus(EmailStatus.SENT);
        assertEquals(1, sentEmails.size());
        EmailOutbox sentEmail = sentEmails.get(0);
        assertEquals(email.getId(), sentEmail.getId());
        assertNotNull(sentEmail.getSentAt());

        // Verify PENDING list is empty
        assertEquals(0, emailOutboxRepository.findByStatus(EmailStatus.PENDING).size());
    }
}
