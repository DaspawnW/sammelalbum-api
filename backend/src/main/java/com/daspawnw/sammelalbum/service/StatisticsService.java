package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.StatisticsDtos.*;
import com.daspawnw.sammelalbum.repository.CardOfferRepository;
import com.daspawnw.sammelalbum.repository.CardSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CardOfferRepository cardOfferRepository;
    private final CardSearchRepository cardSearchRepository;

    public StatisticsResponse getStatistics() {
        // Get card offer statistics
        long totalOffers = cardOfferRepository.count();
        long freeOffers = cardOfferRepository.countByOfferFreebie(true);
        long exchangeOffers = cardOfferRepository.countByOfferExchange(true);
        long paidOffers = cardOfferRepository.countByOfferPayed(true);

        CardOfferStatistics offerStats = new CardOfferStatistics(
                totalOffers,
                freeOffers,
                exchangeOffers,
                paidOffers);

        // Get card search statistics
        long totalSearches = cardSearchRepository.count();
        CardSearchStatistics searchStats = new CardSearchStatistics(totalSearches);

        return new StatisticsResponse(offerStats, searchStats);
    }
}
