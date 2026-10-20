package com.aisc.algoviz.user.service;

import com.aisc.algoviz.auth.service.AuthService;
import com.aisc.algoviz.user.dto.UserDto;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@algoviz.com")
                .fullName("John Doe")
                .avatarUrl("https://example.com/avatar.png")
                .points(100)
                .solvedProblemsCount(5)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("john_doe");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUser returns authenticated user from repository")
    void testGetCurrentUserSuccess() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));

        User user = userService.getCurrentUser();

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("john_doe", user.getUsername());
    }

    @Test
    @DisplayName("getCurrentUser throws RuntimeException if user not found")
    void testGetCurrentUserNotFound() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.getCurrentUser());
        assertEquals("Current user not found", ex.getMessage());
    }

    @Test
    @DisplayName("getCurrentUserProfile delegates mapping to authService")
    void testGetCurrentUserProfile() {
        UserDto dto = UserDto.builder()
                .id(1L)
                .username("john_doe")
                .fullName("John Doe")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(authService.mapToUserDto(sampleUser)).thenReturn(dto);

        UserDto result = userService.getCurrentUserProfile();

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        verify(authService, times(1)).mapToUserDto(sampleUser);
    }

    @Test
    @DisplayName("updateProfile updates both fullName and avatarUrl when valid strings provided")
    void testUpdateProfileBothFields() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile("Jane Doe", "https://new-avatar.png");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("Jane Doe", saved.getFullName());
        assertEquals("https://new-avatar.png", saved.getAvatarUrl());
    }

    @Test
    @DisplayName("updateProfile ignores null or blank parameters, maintaining original values")
    void testUpdateProfileIgnoreNullOrBlank() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile("   ", null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        // Must remain unchanged
        assertEquals("John Doe", saved.getFullName());
        assertEquals("https://example.com/avatar.png", saved.getAvatarUrl());
    }
}
