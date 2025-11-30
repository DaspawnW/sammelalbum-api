package com.daspawnw.sammelalbum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "offerer_id", nullable = false)
    private Long offererId;

    @Column(name = "requested_sticker_id", nullable = false)
    private Long requestedStickerId;

    @Column(name = "offered_sticker_id")
    private Long offeredStickerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_type", nullable = false)
    private ExchangeType exchangeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeStatus status;

    // Foreign key references to specific cards involved in the exchange
    @Column(name = "offerer_card_offer_id")
    private Long offererCardOfferId;

    @Column(name = "requester_card_search_id")
    private Long requesterCardSearchId;

    @Column(name = "requester_card_offer_id")
    private Long requesterCardOfferId;

    @Column(name = "offerer_card_search_id")
    private Long offererCardSearchId;

    @Column(name = "requester_closed", nullable = false)
    @Builder.Default
    private Boolean requesterClosed = false;

    @Column(name = "offerer_closed", nullable = false)
    @Builder.Default
    private Boolean offererClosed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason")
    private CancellationReason cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
