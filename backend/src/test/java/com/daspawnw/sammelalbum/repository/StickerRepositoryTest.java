package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.Sticker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class StickerRepositoryTest {

    @Autowired
    private StickerRepository stickerRepository;

    @Test
    void shouldHave636Stickers() {
        long count = stickerRepository.count();
        assertEquals(636, count);
    }
}
