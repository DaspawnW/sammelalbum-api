package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.model.ExchangeRequest;
import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.ExchangeType;
import com.daspawnw.sammelalbum.repository.ExchangeRequestRepository;
import com.daspawnw.sammelalbum.service.ExchangeService;
import com.daspawnw.sammelalbum.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/match_scenarios.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ExchangeServiceNotificationTest {

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @MockBean
    private NotificationService notificationService;

    @Test
    void processInitialRequests_ShouldSendNotificationsAndUpdateStatus() {
        // Arrange: Create an INITIAL exchange request
        ExchangeRequest request = ExchangeRequest.builder()
                .requesterId(1L)
                .offererId(4L)
                .requestedStickerId(1L)
                .offeredStickerId(6L)
                .exchangeType(ExchangeType.EXCHANGE)
                .status(ExchangeStatus.INITIAL)
                .build();
        exchangeRequestRepository.save(request);
        exchangeRequestRepository.flush(); // Ensure data is written to DB

        // Act: Run the service method
        exchangeService.processInitialRequests();

        // Assert:
        // 1. Verify NotificationService was called for Offerer 4
        org.mockito.ArgumentCaptor<List<String>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(notificationService, times(1)).sendExchangeNotification(eq(4L), captor.capture());

        // Verify the message content
        List<String> messages = captor.getValue();
        assertEquals(1, messages.size());
        String expectedMessage = "Tauschanfrage: Dein Sticker Sticker 1 (ID: 1) gegen Sticker Sticker 6 (ID: 6).";
        assertEquals(expectedMessage, messages.get(0));

        // 2. Verify status is updated to MAIL_SEND
        List<ExchangeRequest> updatedRequests = exchangeRequestRepository.findAll();
        assertEquals(1, updatedRequests.size());
        ExchangeRequest updatedRequest = updatedRequests.get(0);
        assertEquals(ExchangeStatus.MAIL_SEND, updatedRequest.getStatus());
    }

    @Test
    void processInitialRequests_Freebie_ShouldSendCorrectNotification() {
        // Arrange: Create an INITIAL FREEBIE exchange request
        // Requester 1 asks for Sticker 1 from Offerer 4 (Freebie)
        ExchangeRequest request = ExchangeRequest.builder()
                .requesterId(1L)
                .offererId(4L)
                .requestedStickerId(1L)
                .offeredStickerId(null)
                .exchangeType(ExchangeType.FREEBIE)
                .status(ExchangeStatus.INITIAL)
                .build();
        exchangeRequestRepository.save(request);
        exchangeRequestRepository.flush();

        // Act
        exchangeService.processInitialRequests();

        // Assert
        org.mockito.ArgumentCaptor<List<String>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(notificationService, times(1)).sendExchangeNotification(eq(4L), captor.capture());

        List<String> messages = captor.getValue();
        assertEquals(1, messages.size());
        // Expected: "Anfrage (Freebie): Dein Sticker Sticker 1 (ID: 1) wird angefragt."
        String expectedMessage = "Anfrage (Freebie): Dein Sticker Sticker 1 (ID: 1) wird angefragt.";
        assertEquals(expectedMessage, messages.get(0));
    }

    @Test
    void processInitialRequests_NoInitialRequests_ShouldDoNothing() {
        // Arrange: No requests or only non-INITIAL requests
        exchangeRequestRepository.deleteAll();

        // Act
        exchangeService.processInitialRequests();

        // Assert
        verify(notificationService, times(0)).sendExchangeNotification(eq(4L), anyList());
    }
}
