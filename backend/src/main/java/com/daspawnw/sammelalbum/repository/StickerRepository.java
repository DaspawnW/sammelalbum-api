package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Sticker")
    @Override
    void deleteAll();
}
