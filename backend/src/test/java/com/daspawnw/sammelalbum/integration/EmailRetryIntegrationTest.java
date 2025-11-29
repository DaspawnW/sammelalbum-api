package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import com.daspawnw.sammelalbum.model.EmailStatus;
import com.daspawnw.sammelalbum.repository.EmailOutboxRepository;
import com.daspawnw.sammelalbum.scheduler.EmailSenderScheduler;
import com.daspawnw.sammelalbum.service.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/match_scenarios.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class EmailRetryIntegrationTest {

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private EmailSenderScheduler emailSenderScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailOutboxRepository.deleteAll();
        jdbcTemplate.execute("TRUNCATE TABLE shedlock");
    }

    @Test
    void shouldRetryOnFailureWithBackoff() {
        // 1. Create a pending email
        EmailOutbox email = EmailOutbox.builder()
                .recipientEmail("test@example.com")
                .subject("Test")
                .body("Body")
                .status(EmailStatus.PENDING)
                .build();
        emailOutboxRepository.save(email);

        // 2. Mock failure
        doThrow(new RuntimeException("Simulated failure")).when(emailSender).send(any());

        // 3. Run Scheduler (Attempt 1)
        emailSenderScheduler.processPendingEmailsInternal();

        // Verify mock was called
        verify(emailSender, times(1)).send(any());

        // 4. Verify Retry 1
        EmailOutbox updatedEmail = emailOutboxRepository.findById(email.getId()).orElseThrow();
        assertEquals(EmailStatus.PENDING, updatedEmail.getStatus());
        assertEquals(1, updatedEmail.getRetryCount());
        assertNotNull(updatedEmail.getNextRetryAt());
        assertTrue(updatedEmail.getNextRetryAt().isAfter(LocalDateTime.now()));
        assertEquals("Simulated failure", updatedEmail.getErrorMessage());

        // 5. Run Scheduler again (Should NOT process because of backoff)
        reset(emailSender);
        emailSenderScheduler.processPendingEmailsInternal();
        verify(emailSender, never()).send(any());
    }

    @Test
    void shouldMarkAsFailedAfterMaxRetries() {
        // 1. Create a pending email ready for 3rd retry (so it becomes 3rd failure)
        EmailOutbox email = EmailOutbox.builder()
                .recipientEmail("test@example.com")
                .subject("Test")
                .body("Body")
                .status(EmailStatus.PENDING)
                .retryCount(2) // Already retried twice
                .nextRetryAt(LocalDateTime.now().minusMinutes(1)) // Ready to process
                .build();
        emailOutboxRepository.save(email);

        // 2. Mock failure
        doThrow(new RuntimeException("Final failure")).when(emailSender).send(any());

        // 3. Run Scheduler
        emailSenderScheduler.processPendingEmailsInternal();

        // 4. Verify FAILED status
        EmailOutbox updatedEmail = emailOutboxRepository.findById(email.getId()).orElseThrow();
        assertEquals(EmailStatus.FAILED, updatedEmail.getStatus());
        assertEquals(3, updatedEmail.getRetryCount());
        assertEquals("Final failure", updatedEmail.getErrorMessage());
    }

    @Test
    void shouldSendSuccessfullyAfterRetry() {
        // 1. Create a pending email ready for retry
        EmailOutbox email = EmailOutbox.builder()
                .recipientEmail("test@example.com")
                .subject("Test")
                .body("Body")
                .status(EmailStatus.PENDING)
                .retryCount(1)
                .nextRetryAt(LocalDateTime.now().minusMinutes(1))
                .build();
        emailOutboxRepository.save(email);

        // 2. Mock success (default behavior of mock is doNothing)

        // 3. Run Scheduler
        emailSenderScheduler.processPendingEmailsInternal();

        // 4. Verify SENT status
        EmailOutbox updatedEmail = emailOutboxRepository.findById(email.getId()).orElseThrow();
        assertEquals(EmailStatus.SENT, updatedEmail.getStatus());
        assertEquals(1, updatedEmail.getRetryCount()); // Count remains same
        assertNotNull(updatedEmail.getSentAt());
    }
}
