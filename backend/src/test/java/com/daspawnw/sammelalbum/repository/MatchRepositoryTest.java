package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.CardOffer;
import com.daspawnw.sammelalbum.model.CardSearch;
import com.daspawnw.sammelalbum.model.Sticker;
import com.daspawnw.sammelalbum.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class MatchRepositoryTest {

    @Autowired
    private CardOfferRepository cardOfferRepository;

    @Autowired
    private CardSearchRepository cardSearchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StickerRepository stickerRepository;

    private Long aliceId;
    private Long bobId;
    private Long charlieId;

    @BeforeEach
    void setUp() {
        cardOfferRepository.deleteAll();
        cardSearchRepository.deleteAll();
        userRepository.deleteAll();
        stickerRepository.deleteAll();

        // Create Users
        User alice = userRepository
                .save(User.builder().firstname("Alice").lastname("Doe").mail("alice@example.com").build());
        User bob = userRepository.save(User.builder().firstname("Bob").lastname("Doe").mail("bob@example.com").build());
        User charlie = userRepository
                .save(User.builder().firstname("Charlie").lastname("Doe").mail("charlie@example.com").build());

        aliceId = alice.getId();
        bobId = bob.getId();
        charlieId = charlie.getId();

        // Create Stickers
        for (long i = 1; i <= 10; i++) {
            stickerRepository.save(Sticker.builder().id(i).name("Sticker " + i).build());
        }
    }

    @Test
    void findFreebieMatches_ShouldReturnCorrectCounts() {
        // Alice needs 1, 2, 3
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(1L).build());
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(2L).build());
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(3L).build());

        // Bob offers 1, 2 as freebie
        cardOfferRepository.save(CardOffer.builder().userId(bobId).stickerId(1L).offerFreebie(true).build());
        cardOfferRepository.save(CardOffer.builder().userId(bobId).stickerId(2L).offerFreebie(true).build());

        // Charlie offers 1 as freebie
        cardOfferRepository.save(CardOffer.builder().userId(charlieId).stickerId(1L).offerFreebie(true).build());

        Page<MatchProjection> results = cardOfferRepository.findFreebieMatches(aliceId, PageRequest.of(0, 10));

        assertEquals(2, results.getTotalElements());
        List<MatchProjection> content = results.getContent();

        // Bob should be first with 2 matches
        assertEquals(bobId, content.get(0).getUserId());
        assertEquals(2L, content.get(0).getMatchCount());

        // Charlie should be second with 1 match
        assertEquals(charlieId, content.get(1).getUserId());
        assertEquals(1L, content.get(1).getMatchCount());
    }

    @Test
    void findPayedMatches_ShouldReturnCorrectCounts() {
        // Alice needs 1, 2
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(1L).build());
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(2L).build());

        // Bob offers 1 as payed
        cardOfferRepository.save(CardOffer.builder().userId(bobId).stickerId(1L).offerPayed(true).build());

        Page<MatchProjection> results = cardOfferRepository.findPayedMatches(aliceId, PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals(bobId, results.getContent().get(0).getUserId());
        assertEquals(1L, results.getContent().get(0).getMatchCount());
    }

    @Test
    void findExchangeMatches_ShouldReturnCorrectCounts() {
        // Alice needs 1, 2, 3. Offers 4, 5, 6
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(1L).build());
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(2L).build());
        cardSearchRepository.save(CardSearch.builder().userId(aliceId).stickerId(3L).build());

        cardOfferRepository.save(CardOffer.builder().userId(aliceId).stickerId(4L).offerExchange(true).build());
        cardOfferRepository.save(CardOffer.builder().userId(aliceId).stickerId(5L).offerExchange(true).build());
        cardOfferRepository.save(CardOffer.builder().userId(aliceId).stickerId(6L).offerExchange(true).build());

        // Bob needs 4, 5. Offers 1, 2
        // Match: Alice gives 4, 5 (2 cards). Bob gives 1, 2 (2 cards). Total match = 2
        cardSearchRepository.save(CardSearch.builder().userId(bobId).stickerId(4L).build());
        cardSearchRepository.save(CardSearch.builder().userId(bobId).stickerId(5L).build());

        cardOfferRepository.save(CardOffer.builder().userId(bobId).stickerId(1L).offerExchange(true).build());
        cardOfferRepository.save(CardOffer.builder().userId(bobId).stickerId(2L).offerExchange(true).build());

        // Charlie needs 4. Offers 1, 2, 3
        // Match: Alice gives 4 (1 card). Charlie gives 1, 2, 3 (3 cards). Total match =
        // min(1, 3) = 1
        cardSearchRepository.save(CardSearch.builder().userId(charlieId).stickerId(4L).build());

        cardOfferRepository.save(CardOffer.builder().userId(charlieId).stickerId(1L).offerExchange(true).build());
        cardOfferRepository.save(CardOffer.builder().userId(charlieId).stickerId(2L).offerExchange(true).build());
        cardOfferRepository.save(CardOffer.builder().userId(charlieId).stickerId(3L).offerExchange(true).build());

        Page<MatchProjection> results = cardOfferRepository.findExchangeMatches(aliceId, PageRequest.of(0, 10));

        assertEquals(2, results.getTotalElements());
        List<MatchProjection> content = results.getContent();

        // Bob should be first with 2 matches
        assertEquals(bobId, content.get(0).getUserId());
        assertEquals(2L, content.get(0).getMatchCount());

        // Charlie should be second with 1 match
        assertEquals(charlieId, content.get(1).getUserId());
        assertEquals(1L, content.get(1).getMatchCount());
    }
}
