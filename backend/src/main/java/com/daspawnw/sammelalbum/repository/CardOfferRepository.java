package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.CardOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardOfferRepository extends JpaRepository<CardOffer, Long> {
        @org.springframework.data.jpa.repository.Query("""
                        SELECT co
                        FROM CardOffer co
                        JOIN FETCH co.sticker s
                        WHERE co.userId = :userId
                        """)
        List<CardOffer> findAllByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

        List<CardOffer> findByUserIdAndStickerIdIn(Long userId, List<Long> stickerIds);

        @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
                        SELECT user_id AS userId, SUM(cnt) AS matchCount FROM (
                            -- Outgoing: They want my freebies
                            SELECT cs.user_id, COUNT(cs.sticker_id) as cnt
                            FROM card_searches cs
                            JOIN card_offers co ON cs.sticker_id = co.sticker_id
                            WHERE co.user_id = :userId
                              AND co.offer_freebie = TRUE
                              AND cs.user_id != :userId
                              AND co.is_reserved = FALSE
                              AND cs.is_reserved = FALSE
                            GROUP BY cs.user_id

                            UNION ALL

                            -- Incoming: They offer freebies I want
                            SELECT co.user_id, COUNT(co.sticker_id) as cnt
                            FROM card_offers co
                            JOIN card_searches cs ON co.sticker_id = cs.sticker_id
                            WHERE cs.user_id = :userId
                              AND co.offer_freebie = TRUE
                              AND co.user_id != :userId
                              AND co.is_reserved = FALSE
                              AND cs.is_reserved = FALSE
                            GROUP BY co.user_id
                        ) combined
                        GROUP BY user_id
                        ORDER BY matchCount DESC
                        """)
        org.springframework.data.domain.Page<MatchProjection> findFreebieMatches(Long userId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
                        SELECT co.user_id AS userId, COUNT(co.sticker_id) AS matchCount
                        FROM card_offers co
                        JOIN card_searches cs ON co.sticker_id = cs.sticker_id
                        WHERE cs.user_id = :userId
                          AND co.offer_payed = TRUE
                          AND co.user_id != :userId
                          AND co.is_reserved = FALSE
                          AND cs.is_reserved = FALSE
                        GROUP BY co.user_id
                        ORDER BY matchCount DESC
                        """)
        org.springframework.data.domain.Page<MatchProjection> findPayedMatches(Long userId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
                        WITH MyNeeds AS (
                            SELECT sticker_id FROM card_searches WHERE user_id = :userId AND is_reserved = FALSE
                        ),
                        MyOffers AS (
                            SELECT sticker_id FROM card_offers WHERE user_id = :userId AND offer_exchange = TRUE AND is_reserved = FALSE
                        ),
                        PartnerOffers AS (
                            SELECT co.user_id, co.sticker_id
                            FROM card_offers co
                            WHERE co.offer_exchange = TRUE
                              AND co.user_id != :userId
                              AND co.is_reserved = FALSE
                              AND co.sticker_id IN (SELECT sticker_id FROM MyNeeds)
                        ),
                        PartnerNeeds AS (
                            SELECT cs.user_id, cs.sticker_id
                            FROM card_searches cs
                            WHERE cs.user_id != :userId
                              AND cs.is_reserved = FALSE
                              AND cs.sticker_id IN (SELECT sticker_id FROM MyOffers)
                        ),
                        Matches AS (
                            SELECT
                                po.user_id,
                                COUNT(DISTINCT po.sticker_id) AS i_get,
                                COUNT(DISTINCT pn.sticker_id) AS i_give
                            FROM PartnerOffers po
                            JOIN PartnerNeeds pn ON po.user_id = pn.user_id
                            GROUP BY po.user_id
                        )
                        SELECT
                            user_id AS userId,
                            CASE WHEN i_get < i_give THEN i_get ELSE i_give END AS matchCount
                        FROM Matches
                        ORDER BY matchCount DESC
                        """)
        org.springframework.data.domain.Page<MatchProjection> findExchangeMatches(Long userId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("""
                        SELECT co
                        FROM CardOffer co
                        JOIN FETCH co.sticker s
                        WHERE co.userId IN :userIds
                          AND co.stickerId IN (SELECT cs.stickerId FROM CardSearch cs WHERE cs.userId = :currentUserId AND cs.isReserved = FALSE)
                          AND co.isReserved = FALSE
                          AND (
                              (:isFreebie = TRUE AND co.offerFreebie = TRUE) OR
                              (:isPayed = TRUE AND co.offerPayed = TRUE) OR
                              (:isExchange = TRUE AND co.offerExchange = TRUE)
                          )
                        """)
        List<CardOffer> findMatchingOffers(Long currentUserId, List<Long> userIds, boolean isFreebie, boolean isPayed,
                        boolean isExchange);
}
