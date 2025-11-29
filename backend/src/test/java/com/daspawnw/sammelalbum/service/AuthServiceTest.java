package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.AuthDtos.AuthResponse;
import com.daspawnw.sammelalbum.dto.AuthDtos.LoginRequest;
import com.daspawnw.sammelalbum.dto.AuthDtos.RegisterRequest;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import com.daspawnw.sammelalbum.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CredentialsRepository credentialsRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "validationCodes", List.of("CODE-1111"));

        validRegisterRequest = RegisterRequest.builder()
                .username("testuser")
                .password("password")
                .mail("test@example.com")
                .firstname("Test")
                .lastname("User")
                .validationCode("CODE-1111")
                .build();

        validLoginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password")
                .build();
    }

    @Test
    void register_Success() {
        User savedUser = User.builder()
                .id(123L)
                .firstname("Test")
                .lastname("User")
                .mail("test@example.com")
                .build(); // Mock user with ID

        when(credentialsRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByMail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");

        when(credentialsRepository.save(any(Credentials.class))).thenAnswer(invocation -> {
            Credentials c = invocation.getArgument(0);
            // Ensure the user inside credentials has the ID
            if (c.getUser().getId() == null) {
                c.getUser().setId(123L);
            }
            return c;
        });

        when(jwtService.generateToken(any(), eq(123L))).thenReturn("jwt-token");

        AuthResponse response = authService.register(validRegisterRequest);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals("jwt-token", response.getToken());
        assertEquals(123L, response.getUserId());
        verify(credentialsRepository).save(any(Credentials.class));
    }

    @Test
    void register_InvalidCode() {
        validRegisterRequest.setValidationCode("INVALID-CODE");

        assertThrows(IllegalArgumentException.class, () -> authService.register(validRegisterRequest));
        verify(credentialsRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        User user = User.builder().id(456L).build();
        Credentials credentials = Credentials.builder()
                .username("testuser")
                .passwordHash("hashedPassword")
                .user(user)
                .build();

        when(credentialsRepository.findByUsername("testuser")).thenReturn(Optional.of(credentials));
        when(jwtService.generateToken(any(), eq(456L))).thenReturn("jwt-token");

        AuthResponse response = authService.login(validLoginRequest);

        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt-token", response.getToken());
        assertEquals(456L, response.getUserId());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_WrongPassword() {
        // AuthenticationManager throws exception on failure, so we mock that behavior
        doThrow(new IllegalArgumentException("Invalid username or password"))
                .when(authenticationManager).authenticate(any());

        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        assertThrows(IllegalArgumentException.class, () -> authService.login(loginRequest));
    }
}
