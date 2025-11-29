package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class CardOfferRepositoryTest {

        @Autowired
        private CardOfferRepository cardOfferRepository;

        @Autowired
        private UserRepository userRepository;

        @Test
        void findByUserIdAndStickerIdIn_ShouldReturnMatchingOffers() {
                User user1 = userRepository
                                .save(User.builder().firstname("User1").lastname("Test").mail("user1@test.com")
                                                .build());
                User user2 = userRepository
                                .save(User.builder().firstname("User2").lastname("Test").mail("user2@test.com")
                                                .build());
                Long userId = user1.getId();
                Long otherUserId = user2.getId();

                cardOfferRepository.save(CardOffer.builder().userId(userId).stickerId(100L).offerPayed(true).build());
                cardOfferRepository
                                .save(CardOffer.builder().userId(userId).stickerId(200L).offerExchange(true).build());
                cardOfferRepository.save(CardOffer.builder().userId(userId).stickerId(300L).offerFreebie(true).build());
                cardOfferRepository
                                .save(CardOffer.builder().userId(otherUserId).stickerId(100L).offerPayed(true).build()); // Other
                                                                                                                         // user

                List<Long> stickerIds = Arrays.asList(100L, 200L);
                List<CardOffer> results = cardOfferRepository.findByUserIdAndStickerIdIn(userId, stickerIds);

                assertEquals(2, results.size());
                assertTrue(results.stream().anyMatch(o -> o.getStickerId().equals(100L)));
                assertTrue(results.stream().anyMatch(o -> o.getStickerId().equals(200L)));
        }
}
