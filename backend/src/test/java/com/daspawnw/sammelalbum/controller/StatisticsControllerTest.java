package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.dto.StatisticsDtos.*;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.validation-codes=CODE-1111",
        "app.jwt.secret=K7gNU3kef8297wnsJvbdw/Ba49bmGW76NFh70fE0ZeM=",
        "app.jwt.expiration=86400000"
})
@AutoConfigureMockMvc
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatisticsService statisticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private CardOfferRepository cardOfferRepository;

    @Autowired
    private CardSearchRepository cardSearchRepository;

    @BeforeEach
    void setUp() {
        // Clean up database
        cardOfferRepository.deleteAll();
        cardSearchRepository.deleteAll();
        credentialsRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser
    void getStatistics_ShouldReturn200_WhenAuthenticated() throws Exception {
        // Given
        CardOfferStatistics offerStats = new CardOfferStatistics(100, 30, 50, 20);
        CardSearchStatistics searchStats = new CardSearchStatistics(75);
        StatisticsResponse response = new StatisticsResponse(offerStats, searchStats);

        when(statisticsService.getStatistics()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardOffers.total").value(100))
                .andExpect(jsonPath("$.cardOffers.free").value(30))
                .andExpect(jsonPath("$.cardOffers.exchange").value(50))
                .andExpect(jsonPath("$.cardOffers.paid").value(20))
                .andExpect(jsonPath("$.cardSearches.total").value(75));

        verify(statisticsService).getStatistics();
    }

    @Test
    void getStatistics_ShouldReturn401_WhenUnauthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isUnauthorized());

        verify(statisticsService, never()).getStatistics();
    }

    @Test
    @WithMockUser
    void getStatistics_ShouldReturnZeroCounts_WhenNoData() throws Exception {
        // Given
        CardOfferStatistics offerStats = new CardOfferStatistics(0, 0, 0, 0);
        CardSearchStatistics searchStats = new CardSearchStatistics(0);
        StatisticsResponse response = new StatisticsResponse(offerStats, searchStats);

        when(statisticsService.getStatistics()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardOffers.total").value(0))
                .andExpect(jsonPath("$.cardOffers.free").value(0))
                .andExpect(jsonPath("$.cardOffers.exchange").value(0))
                .andExpect(jsonPath("$.cardOffers.paid").value(0))
                .andExpect(jsonPath("$.cardSearches.total").value(0));
    }
}
