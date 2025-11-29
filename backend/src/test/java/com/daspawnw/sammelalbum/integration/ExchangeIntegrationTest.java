package com.daspawnw.sammelalbum.integration;

import com.daspawnw.sammelalbum.model.ExchangeStatus;
import com.daspawnw.sammelalbum.model.ExchangeType;
import com.daspawnw.sammelalbum.security.CustomUserDetails;
import com.daspawnw.sammelalbum.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@Sql(scripts = "/match_scenarios.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ExchangeIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private ObjectMapper objectMapper;

        private String mainUserToken;

        @BeforeEach
        void setup() {
                // Generate token for Main User (ID 1)
                var userDetails = new CustomUserDetails(
                                "mainuser", "password", Collections.emptyList(), 1L);
                mainUserToken = "Bearer " + jwtService.generateToken(userDetails, 1L);
        }

        @Test
        void createExchangeRequest_ExchangeType_ShouldSuccess() throws Exception {
                // User 1 requests exchange with User 4 (Perfect Match)
                // User 1 needs Sticker 1 (Offered by User 4 as Exchange)
                // User 4 needs Sticker 6 (Offered by User 1 as Exchange)

                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 4,
                                                "requestedStickerId", 1,
                                                "offeredStickerId", 6,
                                                "exchangeType", ExchangeType.EXCHANGE))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.requesterId").value(1))
                                .andExpect(jsonPath("$.offererId").value(4))
                                .andExpect(jsonPath("$.requestedStickerId").value(1))
                                .andExpect(jsonPath("$.offeredStickerId").value(6))
                                .andExpect(jsonPath("$.exchangeType").value(ExchangeType.EXCHANGE.name()))
                                .andExpect(jsonPath("$.status").value(ExchangeStatus.INITIAL.name()));
        }

        @Test
        void createExchangeRequest_ExchangeType_MissingOfferedSticker_ShouldFail() throws Exception {
                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 4,
                                                "requestedStickerId", 1,
                                                // Missing offeredStickerId
                                                "exchangeType", ExchangeType.EXCHANGE))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createExchangeRequest_PayedType_ShouldSuccess() throws Exception {
                // User 7 offers Sticker 1 as PAYED.
                // User 1 needs Sticker 1.

                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 7,
                                                "requestedStickerId", 1,
                                                "exchangeType", ExchangeType.PAYED))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.exchangeType").value(ExchangeType.PAYED.name()));
        }

        @Test
        void createExchangeRequest_PayedType_WrongType_Freebie_ShouldFail() throws Exception {
                // User 7 offers Sticker 2 as FREEBIE, NOT PAYED.
                // Requesting it as PAYED should fail.

                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 7,
                                                "requestedStickerId", 2,
                                                "exchangeType", ExchangeType.PAYED))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createExchangeRequest_FreebieType_ShouldSuccess() throws Exception {
                // User 7 offers Sticker 2 as FREEBIE.
                // User 1 needs Sticker 2.

                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 7,
                                                "requestedStickerId", 2,
                                                "exchangeType", ExchangeType.FREEBIE))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.exchangeType").value(ExchangeType.FREEBIE.name()));
        }

        @Test
        void createExchangeRequest_FreebieType_WrongType_ShouldFail() throws Exception {
                // User 7 offers Sticker 1 as PAYED, NOT FREEBIE.
                // Requesting it as FREEBIE should fail.

                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 7,
                                                "requestedStickerId", 1,
                                                "exchangeType", ExchangeType.FREEBIE))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createExchangeRequest_PayedType_WrongType_Exchange_ShouldFail() throws Exception {
                // User 7 offers Sticker 3 as EXCHANGE, NOT PAYED.
                // Requesting it as PAYED should fail.

                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 7,
                                                "requestedStickerId", 3,
                                                "exchangeType", ExchangeType.PAYED))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createExchangeRequest_ExchangeType_InvalidMatch_ShouldFail() throws Exception {
                // User 6 (No Match) offers Sticker 11, but Main doesn't need it?
                // Wait, Main needs 1,2,3,4,5. User 6 offers 11. Main doesn't need 11.
                // So this is not a match for Main.

                // Or try to exchange with User 4 but offer something User 4 doesn't need.
                // User 4 needs 6, 7, 8. Main offers 6, 7, 8, 9, 10.
                // If Main offers 9 (User 4 doesn't need), it should fail.

                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 4,
                                                "requestedStickerId", 1,
                                                "offeredStickerId", 9,
                                                "exchangeType", ExchangeType.EXCHANGE))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createExchangeRequest_PayedType_WithOfferedSticker_ShouldFail() throws Exception {
                // PAYED type must NOT have offeredStickerId
                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 7,
                                                "requestedStickerId", 1,
                                                "offeredStickerId", 999, // Should not be here
                                                "exchangeType", ExchangeType.PAYED))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createExchangeRequest_FreebieType_WithOfferedSticker_ShouldFail() throws Exception {
                // FREEBIE type must NOT have offeredStickerId
                mockMvc.perform(post("/api/exchanges")
                                .header("Authorization", mainUserToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "offererId", 7,
                                                "requestedStickerId", 2,
                                                "offeredStickerId", 999, // Should not be here
                                                "exchangeType", ExchangeType.FREEBIE))))
                                .andExpect(status().isBadRequest());
        }
}
