package com.datingapp.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.datingapp.model.User;
import com.datingapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void register_shouldCreateUser_whenUsernameIsAvailable() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register("testuser", "password123", "Test User", 25, "Male", "Bio");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("hashedPassword", result.getPasswordHash());
        assertEquals("Test User", result.getDisplayName());
        assertEquals(25, result.getAge());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenUsernameIsTaken() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.register("existinguser", "password123", "Test", 25, "Male", null));

        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_shouldReturnUser_whenExists() {
        User mockUser = User.builder().username("testuser").displayName("Test").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenNotExists() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void isUsernameAvailable_shouldReturnTrue_whenNotTaken() {
        when(userRepository.existsByUsername("available")).thenReturn(false);

        assertTrue(userService.isUsernameAvailable("available"));
    }

    @Test
    void isUsernameAvailable_shouldReturnFalse_whenTaken() {
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertFalse(userService.isUsernameAvailable("taken"));
    }
}
