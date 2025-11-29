package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.CardSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardSearchRepository extends JpaRepository<CardSearch, Long> {
    List<CardSearch> findAllByUserId(Long userId);

    List<CardSearch> findByUserIdAndStickerIdIn(Long userId, List<Long> stickerIds);
}
