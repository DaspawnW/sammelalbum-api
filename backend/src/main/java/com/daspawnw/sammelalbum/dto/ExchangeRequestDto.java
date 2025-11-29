package com.daspawnw.sammelalbum.dto;

import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.ExchangeType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExchangeRequestDto {
    private Long id;
    private Long requesterId;
    private Long offererId;
    private Long requestedStickerId;
    private Long offeredStickerId;
    private ExchangeType exchangeType;
    private ExchangeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Conditional fields
    private String partnerFirstname;
    private String partnerLastname;
    private String partnerContact;
}
