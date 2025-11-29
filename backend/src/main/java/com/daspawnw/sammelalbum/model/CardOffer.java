package com.daspawnw.sammelalbum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sticker_id", nullable = false)
    private Long stickerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sticker_id", insertable = false, updatable = false)
    private Sticker sticker;

    @Column(name = "offer_payed", nullable = false)
    @Builder.Default
    private Boolean offerPayed = false;

    @Column(name = "offer_freebie", nullable = false)
    @Builder.Default
    private Boolean offerFreebie = false;

    @Column(name = "offer_exchange", nullable = false)
    @Builder.Default
    private Boolean offerExchange = false;

    @Column(name = "is_reserved", nullable = false)
    @Builder.Default
    private Boolean isReserved = false;
}
