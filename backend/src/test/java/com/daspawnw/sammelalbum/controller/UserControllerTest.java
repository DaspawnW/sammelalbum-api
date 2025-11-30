package com.daspawnw.sammelalbum.controller;

import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daspawnw.sammelalbum.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;

@SpringBootTest(properties = {
                "app.validation-codes=CODE-1111",
                "app.jwt.secret=K7gNU3kef8297wnsJvbdw/Ba49bmGW76NFh70fE0ZeM=",
                "app.jwt.expiration=86400000"
})
@AutoConfigureMockMvc
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private com.daspawnw.sammelalbum.repository.CredentialsRepository credentialsRepository;

        @BeforeEach
        void setUp() {
                credentialsRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        void getMe_ShouldReturnCurrentUser_WhenAuthenticated() throws Exception {
                User user = User.builder()
                                .firstname("Test")
                                .lastname("User")
                                .mail("test@example.com")
                                .contact("test@contact.com")
                                .build();
                user = userRepository.save(user);

                CustomUserDetails userDetails = new CustomUserDetails(
                                user.getMail(),
                                "password",
                                Collections.emptyList(),
                                user.getId());

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));

                mockMvc.perform(get("/api/user/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstname").value("Test"))
                                .andExpect(jsonPath("$.lastname").value("User"))
                                .andExpect(jsonPath("$.mail").value("test@example.com"))
                                .andExpect(jsonPath("$.contact").value("test@contact.com"));
        }

        @Test
        void getMe_ShouldReturn403_WhenUnauthenticated() throws Exception {
                mockMvc.perform(get("/api/user/me"))
                                .andExpect(status().isForbidden());
        }
}
