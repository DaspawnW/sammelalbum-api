package com.daspawnw.sammelalbum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card_searches")
public class CardSearch {

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

    @Column(name = "is_reserved", nullable = false)
    @Builder.Default
    private Boolean isReserved = false;
}
