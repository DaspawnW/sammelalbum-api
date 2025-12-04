package com.daspawnw.sammelalbum.dto;

public class StatisticsDtos {

    public record StatisticsResponse(
            CardOfferStatistics cardOffers,
            CardSearchStatistics cardSearches) {
    }

    public record CardOfferStatistics(
            long total,
            long free,
            long exchange,
            long paid) {
    }

    public record CardSearchStatistics(
            long total) {
    }
}
