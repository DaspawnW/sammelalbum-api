package com.daspawnw.sammelalbum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CardOfferDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardOfferRequest {
        private Long stickerId;
        private Boolean offerPayed;
        private Boolean offerFreebie;
        private Boolean offerExchange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkCardOfferRequest {
        private List<Long> stickerIds;
        private Boolean offerPayed;
        private Boolean offerFreebie;
        private Boolean offerExchange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUpdateOfferRequest {
        private List<Long> stickerIds;
        private Boolean offerPayed;
        private Boolean offerFreebie;
        private Boolean offerExchange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardOfferResponse {
        private Long id;
        private Long stickerId;
        private Boolean offerPayed;
        private Boolean offerFreebie;
        private Boolean offerExchange;
        private Boolean isReserved;
    }
}
