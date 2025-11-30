package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.CardSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardSearchRepository extends JpaRepository<CardSearch, Long> {
        @org.springframework.data.jpa.repository.Query("""
                        SELECT cs
                        FROM CardSearch cs
                        JOIN FETCH cs.sticker s
                        WHERE cs.userId = :userId
                        """)
        List<CardSearch> findAllByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

        List<CardSearch> findByUserIdAndStickerIdIn(Long userId, List<Long> stickerIds);

        @org.springframework.data.jpa.repository.Query("""
                        SELECT cs
                        FROM CardSearch cs
                        JOIN FETCH cs.sticker s
                        WHERE cs.userId IN :userIds
                          AND cs.stickerId IN (SELECT co.stickerId FROM CardOffer co WHERE co.userId = :currentUserId AND co.offerExchange = TRUE AND co.isReserved = FALSE)
                          AND cs.isReserved = FALSE
                        """)
        List<CardSearch> findMatchingSearches(
                        @org.springframework.data.repository.query.Param("userIds") List<Long> userIds,
                        @org.springframework.data.repository.query.Param("currentUserId") Long currentUserId);
}
