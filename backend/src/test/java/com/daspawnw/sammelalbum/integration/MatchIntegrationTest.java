package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
                "app.validation-codes=CODE1,CODE2",
                "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                "app.jwt.expiration=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MatchIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

        @Autowired
        private javax.sql.DataSource dataSource;

        private String mainUserToken;

        @BeforeEach
        void setup() throws java.sql.SQLException {
                // 1. Cleanup
                jdbcTemplate.execute("DELETE FROM email_outbox");
                jdbcTemplate.execute("DELETE FROM exchange_requests");
                jdbcTemplate.execute("DELETE FROM card_searches");
                jdbcTemplate.execute("DELETE FROM card_offers");
                jdbcTemplate.execute("DELETE FROM credentials");
                jdbcTemplate.execute("DELETE FROM users");
                jdbcTemplate.execute("DELETE FROM stickers");

                // 2. Load Data
                java.sql.Connection conn = org.springframework.jdbc.datasource.DataSourceUtils
                                .getConnection(dataSource);
                try {
                        org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(conn,
                                        new org.springframework.core.io.ClassPathResource("match_scenarios.sql"));
                } finally {
                        org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(conn, dataSource);
                }

                // Generate token for Main User (ID 1 from SQL)
                var userDetails = new CustomUserDetails(
                                "mainuser", "password", Collections.emptyList(), 1L);
                mainUserToken = "Bearer " + jwtService.generateToken(userDetails, 1L);
        }

        @Test
        void getFreebieMatches_ShouldReturnCorrectOrderAndDetails() throws Exception {
                // Expectation:
                // 1. User 2 (Freebie King): 3 matches (Stickers 1, 2, 3)
                // 2. User 3 (Payed Merchant): 1 match (Sticker 4)

                mockMvc.perform(get("/api/matches/freebie")
                                .header("Authorization", mainUserToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                // First Result: User 2
                                .andExpect(jsonPath("$.content[0].userId").value(2)) // Freebie King
                                .andExpect(jsonPath("$.content[0].matchCount").value(5)) // 5 matches (Stickers 1-5)
                                .andExpect(jsonPath("$.content[0].matches", hasSize(5)))
                                .andExpect(jsonPath("$.content[0].matches[0].id").value(1))
                                .andExpect(jsonPath("$.content[0].matches[0].name").value("Sticker 1"))
                                // Second Result: User 7 (One Way) - Offers Sticker 2 as Freebie
                                .andExpect(jsonPath("$.content[1].userId").value(7))
                                .andExpect(jsonPath("$.content[1].matchCount").value(1))
                                .andExpect(jsonPath("$.content[1].matches", hasSize(1)))
                                .andExpect(jsonPath("$.content[1].matches[0].id").value(2))
                                .andExpect(jsonPath("$.content[1].matches[0].name").value("Sticker 2"));
        }

        @Test
        void getPayedMatches_ShouldReturnCorrectOrderAndDetails() throws Exception {
                mockMvc.perform(get("/api/matches/payed")
                                .header("Authorization", mainUserToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2))) // Payed Merchant, One Way
                                .andExpect(jsonPath("$.content[0].userId").value(3)) // Payed Merchant
                                .andExpect(jsonPath("$.content[0].matchCount").value(5))
                                .andExpect(jsonPath("$.content[1].userId").value(7)) // One Way - Offers Sticker 1 as
                                                                                     // Payed
                                .andExpect(jsonPath("$.content[1].matchCount").value(1));
        }

        @Test
        void getExchangeMatches_ShouldReturnCorrectOrderAndDetails() throws Exception {
                // Expectation:
                // 1. User 4 (Perfect Match): 3 matches (Offers 1, 2, 3; Needs 6, 7, 8) ->
                // min(3, 3) = 3
                // 2. User 5 (Partial Match): 1 match (Offers 1, 2; Needs 6) -> min(2, 1) = 1
                // User 7 (One Way) should NOT appear because match count is 0.

                mockMvc.perform(get("/api/matches/exchange")
                                .header("Authorization", mainUserToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                // First Result: User 4
                                .andExpect(jsonPath("$.content[0].userId").value(4))
                                .andExpect(jsonPath("$.content[0].matchCount").value(3))
                                .andExpect(jsonPath("$.content[0].matches", hasSize(3)))
                                .andExpect(jsonPath("$.content[0].matches[*].id", containsInAnyOrder(1, 2, 3)))
                                // Second Result: User 5
                                .andExpect(jsonPath("$.content[1].userId").value(5))
                                .andExpect(jsonPath("$.content[1].matchCount").value(1))
                                .andExpect(jsonPath("$.content[1].matches", hasSize(2))) // Note: Matches list contains
                                                                                         // ALL matching
                                                                                         // offers (1, 2), but
                                                                                         // matchCount is 1 (exchange
                                                                                         // potential)
                                .andExpect(jsonPath("$.content[1].matches[*].id", containsInAnyOrder(1, 2)));
        }

        @Test
        void getExchangeMatches_ForUser4_ShouldFindMainUser() throws Exception {
                // User 4 (Perfect Match) searching for exchange matches.
                // Should find User 1 (Main User).
                // User 4 Needs: 6, 7, 8 (User 1 Offers) -> i_get = 3
                // User 4 Offers: 1, 2, 3 (User 1 Needs) -> i_give = 3
                // Match Count: 3

                // Generate token for User 4
                var userDetails = new CustomUserDetails(
                                "perfectmatch", "password", Collections.emptyList(), 4L);
                String user4Token = "Bearer " + jwtService.generateToken(userDetails, 4L);

                mockMvc.perform(get("/api/matches/exchange")
                                .header("Authorization", user4Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].userId").value(1))
                                .andExpect(jsonPath("$.content[0].matchCount").value(3))
                                .andExpect(jsonPath("$.content[0].matches", hasSize(3)))
                                .andExpect(jsonPath("$.content[0].matches[*].id", containsInAnyOrder(6, 7, 8)));
        }

        @Test
        void getExchangeMatches_ForUser5_ShouldFindMainUser() throws Exception {
                // User 5 (Partial Match) searching for exchange matches.
                // Should find User 1 (Main User).
                // User 5 Needs: 6 (User 1 Offers) -> i_get = 1
                // User 5 Offers: 1, 2 (User 1 Needs) -> i_give = 2
                // Match Count: min(1, 2) = 1

                // Generate token for User 5
                var userDetails = new CustomUserDetails(
                                "partialmatch", "password", Collections.emptyList(), 5L);
                String user5Token = "Bearer " + jwtService.generateToken(userDetails, 5L);

                mockMvc.perform(get("/api/matches/exchange")
                                .header("Authorization", user5Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].userId").value(1))
                                .andExpect(jsonPath("$.content[0].matchCount").value(1))
                                .andExpect(jsonPath("$.content[0].matches", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].matches[0].id").value(6));
        }

        @Test
        void getFreebieMatches_ForUser6_ShouldReturnEmpty() throws Exception {
                // User 6 (No Match) has no needs that match any offers.
                // Should return empty list.

                var userDetails = new CustomUserDetails(
                                "nomatch", "password", Collections.emptyList(), 6L);
                String user6Token = "Bearer " + jwtService.generateToken(userDetails, 6L);

                mockMvc.perform(get("/api/matches/freebie")
                                .header("Authorization", user6Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(0)))
                                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void getExchangeMatches_ForUser6_ShouldReturnEmpty() throws Exception {
                // User 6 (No Match) has no mutual matches with anyone.
                // Should return empty list.

                var userDetails = new CustomUserDetails(
                                "nomatch", "password", Collections.emptyList(), 6L);
                String user6Token = "Bearer " + jwtService.generateToken(userDetails, 6L);

                mockMvc.perform(get("/api/matches/exchange")
                                .header("Authorization", user6Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(0)))
                                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void getExchangeMatches_ForUser7_ShouldReturnEmpty() throws Exception {
                // User 7 (One Way) offers what User 1 needs, but needs nothing from User 1.
                // Exchange match count should be 0 (min(1, 0) = 0).
                // Should return empty list (or at least NOT contain User 1).

                var userDetails = new CustomUserDetails(
                                "oneway", "password", Collections.emptyList(), 7L);
                String user7Token = "Bearer " + jwtService.generateToken(userDetails, 7L);

                mockMvc.perform(get("/api/matches/exchange")
                                .header("Authorization", user7Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(0)))
                                .andExpect(jsonPath("$.totalElements").value(0));
        }
}
