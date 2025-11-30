package com.daspawnw.sammelalbum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CardSearchDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardSearchRequest {
        private Long stickerId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkCardSearchRequest {
        private List<Long> stickerIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardSearchResponse {
        private Long id;
        private Long stickerId;
        private String stickerName;
        private Boolean isReserved;
    }
}
