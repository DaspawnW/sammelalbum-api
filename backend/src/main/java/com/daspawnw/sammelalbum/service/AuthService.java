package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.AuthDtos.AuthResponse;
import com.daspawnw.sammelalbum.dto.AuthDtos.LoginRequest;
import com.daspawnw.sammelalbum.dto.AuthDtos.RegisterRequest;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final CredentialsRepository credentialsRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        @Value("${app.validation-codes}")
        private List<String> validationCodes;

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (!validationCodes.contains(request.getValidationCode())) {
                        throw new IllegalArgumentException("Invalid validation code");
                }

                if (credentialsRepository.findByUsername(request.getUsername()).isPresent()) {
                        throw new IllegalArgumentException("Username already taken");
                }
                if (userRepository.findByMail(request.getMail()).isPresent()) {
                        throw new IllegalArgumentException("Mail already registered");
                }

                User user = User.builder()
                                .firstname(request.getFirstname())
                                .lastname(request.getLastname())
                                .mail(request.getMail())
                                .contact(request.getContact())
                                .build();

                Credentials credentials = Credentials.builder()
                                .username(request.getUsername())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .user(user)
                                .build();

                var savedCredentials = credentialsRepository.save(credentials);
                var savedUser = savedCredentials.getUser();

                // Generate token for immediate login after registration if desired, or just
                // success message
                // For now, just success message as per original implementation, but we could
                // return token.
                // Let's return a token to be nice.
                var userDetails = new org.springframework.security.core.userdetails.User(
                                savedCredentials.getUsername(),
                                savedCredentials.getPasswordHash(),
                                Collections.emptyList());
                var jwtToken = jwtService.generateToken(userDetails, savedUser.getId());

                return AuthResponse.builder()
                                .token(jwtToken)
                                .message("User registered successfully")
                                .userId(savedUser.getId())
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                var credentials = credentialsRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

                var user = new org.springframework.security.core.userdetails.User(
                                credentials.getUsername(),
                                credentials.getPasswordHash(),
                                Collections.emptyList());
                var jwtToken = jwtService.generateToken(user, credentials.getUser().getId());

                return AuthResponse.builder()
                                .token(jwtToken)
                                .message("Login successful")
                                .userId(credentials.getUser().getId())
                                .build();
        }
}
