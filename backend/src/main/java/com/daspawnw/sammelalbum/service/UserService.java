package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.ChangePasswordRequest;
import com.daspawnw.sammelalbum.dto.UpdateProfileRequest;
import com.daspawnw.sammelalbum.dto.UserDto;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserDto.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .mail(user.getMail())
                .contact(user.getContact())
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstname() != null) {
            user.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            user.setLastname(request.getLastname());
        }
        if (request.getMail() != null) {
            if (!user.getMail().equals(request.getMail()) && userRepository.findByMail(request.getMail()).isPresent()) {
                throw new IllegalArgumentException("Mail already registered");
            }
            user.setMail(request.getMail());
        }
        if (request.getContact() != null) {
            user.setContact(request.getContact());
        }

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        Credentials credentials = credentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User credentials not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), credentials.getPasswordHash())) {
            throw new RuntimeException("Invalid current password");
        }

        credentials.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        credentialsRepository.save(credentials);
    }
}
