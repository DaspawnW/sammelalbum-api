package com.daspawnw.sammelalbum.service;

import com.daspawnw.sammelalbum.dto.ChangePasswordRequest;
import com.daspawnw.sammelalbum.dto.UpdateProfileRequest;
import com.daspawnw.sammelalbum.model.Credentials;
import com.daspawnw.sammelalbum.model.User;
import com.daspawnw.sammelalbum.repository.CredentialsRepository;
import com.daspawnw.sammelalbum.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.daspawnw.sammelalbum.dto.UserDto;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private CredentialsRepository credentialsRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private UserService userService;

        @Test
        void getUserProfile_ShouldReturnUserDto_WhenUserExists() {
                // Arrange
                Long userId = 1L;
                User user = User.builder()
                                .id(userId)
                                .firstname("First")
                                .lastname("Last")
                                .mail("mail@example.com")
                                .contact("Contact")
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                // Act
                UserDto result = userService.getUserProfile(userId);

                // Assert
                assertEquals(userId, result.getId());
                assertEquals("First", result.getFirstname());
                assertEquals("Last", result.getLastname());
                assertEquals("mail@example.com", result.getMail());
                assertEquals("Contact", result.getContact());
        }

        @Test
        void getUserProfile_ShouldThrowException_WhenUserNotFound() {
                // Arrange
                Long userId = 1L;
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(RuntimeException.class, () -> userService.getUserProfile(userId));
        }

        @Test
        void updateProfile_ShouldUpdateFields_WhenUserExists() {
                // Arrange
                Long userId = 1L;
                UpdateProfileRequest request = UpdateProfileRequest.builder()
                                .firstname("NewFirst")
                                .lastname("NewLast")
                                .mail("new@mail.com")
                                .contact("NewContact")
                                .build();

                User user = User.builder()
                                .id(userId)
                                .firstname("OldFirst")
                                .lastname("OldLast")
                                .mail("old@mail.com")
                                .contact("OldContact")
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                // Act
                userService.updateProfile(userId, request);

                // Assert
                verify(userRepository).save(user);
                assertEquals("NewFirst", user.getFirstname());
                assertEquals("NewLast", user.getLastname());
                assertEquals("new@mail.com", user.getMail());
                assertEquals("NewContact", user.getContact());
        }

        @Test
        void updateProfile_ShouldUpdateMail_WhenMailIsNewAndUnique() {
                // Arrange
                Long userId = 1L;
                UpdateProfileRequest request = UpdateProfileRequest.builder()
                                .mail("unique@mail.com")
                                .build();

                User user = User.builder()
                                .id(userId)
                                .mail("old@mail.com")
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(userRepository.findByMail("unique@mail.com")).thenReturn(Optional.empty());

                // Act
                userService.updateProfile(userId, request);

                // Assert
                verify(userRepository).findByMail("unique@mail.com");
                verify(userRepository).save(user);
                assertEquals("unique@mail.com", user.getMail());
        }

        @Test
        void updateProfile_ShouldUpdateOnlyProvidedFields() {
                // Arrange
                Long userId = 1L;
                UpdateProfileRequest request = UpdateProfileRequest.builder()
                                .firstname("NewFirst")
                                .build(); // Only firstname provided

                User user = User.builder()
                                .id(userId)
                                .firstname("OldFirst")
                                .lastname("OldLast")
                                .mail("old@mail.com")
                                .contact("OldContact")
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                // Act
                userService.updateProfile(userId, request);

                // Assert
                verify(userRepository).save(user);
                assertEquals("NewFirst", user.getFirstname());
                assertEquals("OldLast", user.getLastname()); // Should remain unchanged
        }

        @Test
        void updateProfile_ShouldThrowException_WhenMailAlreadyExists() {
                // Arrange
                Long userId = 1L;
                UpdateProfileRequest request = UpdateProfileRequest.builder()
                                .mail("existing@mail.com")
                                .build();

                User user = User.builder()
                                .id(userId)
                                .mail("old@mail.com")
                                .build();

                User existingUser = User.builder()
                                .id(2L)
                                .mail("existing@mail.com")
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(userRepository.findByMail("existing@mail.com")).thenReturn(Optional.of(existingUser));

                // Act & Assert
                assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(userId, request));
                verify(userRepository, never()).save(any());
        }

        @Test
        void updateProfile_ShouldNotThrowException_WhenMailIsSameAsCurrent() {
                // Arrange
                Long userId = 1L;
                UpdateProfileRequest request = UpdateProfileRequest.builder()
                                .mail("same@mail.com")
                                .build();

                User user = User.builder()
                                .id(userId)
                                .mail("same@mail.com")
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                // Act
                userService.updateProfile(userId, request);

                // Assert
                verify(userRepository).save(user);
        }

        @Test
        void updateProfile_ShouldThrowException_WhenUserNotFound() {
                // Arrange
                Long userId = 1L;
                UpdateProfileRequest request = new UpdateProfileRequest();
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(RuntimeException.class, () -> userService.updateProfile(userId, request));
                verify(userRepository, never()).save(any());
        }

        @Test
        void changePassword_ShouldUpdatePassword_WhenCurrentPasswordIsCorrect() {
                // Arrange
                Long userId = 1L;
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("oldPass")
                                .newPassword("newPass")
                                .build();

                Credentials credentials = Credentials.builder()
                                .id(1L)
                                .passwordHash("encodedOldPass")
                                .build();

                when(credentialsRepository.findByUserId(userId)).thenReturn(Optional.of(credentials));
                when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
                when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

                // Act
                userService.changePassword(userId, request);

                // Assert
                verify(credentialsRepository).save(credentials);
                assertEquals("encodedNewPass", credentials.getPasswordHash());
        }

        @Test
        void changePassword_ShouldThrowException_WhenCurrentPasswordIsIncorrect() {
                // Arrange
                Long userId = 1L;
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("wrongPass")
                                .newPassword("newPass")
                                .build();

                Credentials credentials = Credentials.builder()
                                .id(1L)
                                .passwordHash("encodedOldPass")
                                .build();

                when(credentialsRepository.findByUserId(userId)).thenReturn(Optional.of(credentials));
                when(passwordEncoder.matches("wrongPass", "encodedOldPass")).thenReturn(false);

                // Act & Assert
                assertThrows(RuntimeException.class, () -> userService.changePassword(userId, request));
                verify(credentialsRepository, never()).save(any());
        }

        @Test
        void changePassword_ShouldThrowException_WhenCredentialsNotFound() {
                // Arrange
                Long userId = 1L;
                ChangePasswordRequest request = new ChangePasswordRequest();
                when(credentialsRepository.findByUserId(userId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(RuntimeException.class, () -> userService.changePassword(userId, request));
                verify(credentialsRepository, never()).save(any());
        }
}
