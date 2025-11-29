package com.daspawnw.sammelalbum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class MatchDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchResponse {
        private Long userId;
        private Long matchCount;
        private List<MatchStickerDto> matches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchStickerDto {
        private Long id;
        private String name;
    }
}
