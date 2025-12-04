package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.StatisticsDtos.*;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private CardOfferRepository cardOfferRepository;

    @Mock
    private CardSearchRepository cardSearchRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(cardOfferRepository, cardSearchRepository);
    }

    @Test
    void getStatistics_ShouldReturnCorrectCounts() {
        // Given
        when(cardOfferRepository.count()).thenReturn(100L);
        when(cardOfferRepository.countByOfferFreebie(true)).thenReturn(30L);
        when(cardOfferRepository.countByOfferExchange(true)).thenReturn(50L);
        when(cardOfferRepository.countByOfferPayed(true)).thenReturn(20L);
        when(cardSearchRepository.count()).thenReturn(75L);

        // When
        StatisticsResponse response = statisticsService.getStatistics();

        // Then
        assertNotNull(response);

        // Verify card offer statistics
        CardOfferStatistics offerStats = response.cardOffers();
        assertEquals(100L, offerStats.total());
        assertEquals(30L, offerStats.free());
        assertEquals(50L, offerStats.exchange());
        assertEquals(20L, offerStats.paid());

        // Verify card search statistics
        CardSearchStatistics searchStats = response.cardSearches();
        assertEquals(75L, searchStats.total());

        // Verify all repository methods were called
        verify(cardOfferRepository).count();
        verify(cardOfferRepository).countByOfferFreebie(true);
        verify(cardOfferRepository).countByOfferExchange(true);
        verify(cardOfferRepository).countByOfferPayed(true);
        verify(cardSearchRepository).count();
    }

    @Test
    void getStatistics_ShouldReturnZeroCounts_WhenNoData() {
        // Given
        when(cardOfferRepository.count()).thenReturn(0L);
        when(cardOfferRepository.countByOfferFreebie(true)).thenReturn(0L);
        when(cardOfferRepository.countByOfferExchange(true)).thenReturn(0L);
        when(cardOfferRepository.countByOfferPayed(true)).thenReturn(0L);
        when(cardSearchRepository.count()).thenReturn(0L);

        // When
        StatisticsResponse response = statisticsService.getStatistics();

        // Then
        assertNotNull(response);
        assertEquals(0L, response.cardOffers().total());
        assertEquals(0L, response.cardOffers().free());
        assertEquals(0L, response.cardOffers().exchange());
        assertEquals(0L, response.cardOffers().paid());
        assertEquals(0L, response.cardSearches().total());
    }

    @Test
    void getStatistics_ShouldHandleLargeCounts() {
        // Given
        when(cardOfferRepository.count()).thenReturn(1000000L);
        when(cardOfferRepository.countByOfferFreebie(true)).thenReturn(300000L);
        when(cardOfferRepository.countByOfferExchange(true)).thenReturn(500000L);
        when(cardOfferRepository.countByOfferPayed(true)).thenReturn(200000L);
        when(cardSearchRepository.count()).thenReturn(750000L);

        // When
        StatisticsResponse response = statisticsService.getStatistics();

        // Then
        assertNotNull(response);
        assertEquals(1000000L, response.cardOffers().total());
        assertEquals(300000L, response.cardOffers().free());
        assertEquals(500000L, response.cardOffers().exchange());
        assertEquals(200000L, response.cardOffers().paid());
        assertEquals(750000L, response.cardSearches().total());
    }
}
