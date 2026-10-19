package com.aisc.algoviz.auth.service;

import com.aisc.algoviz.auth.config.JwtUtils;
import com.aisc.algoviz.auth.dto.AuthResponse;
import com.aisc.algoviz.auth.dto.LoginRequest;
import com.aisc.algoviz.auth.dto.RefreshTokenRequest;
import com.aisc.algoviz.auth.dto.RegisterRequest;
import com.aisc.algoviz.auth.entity.RefreshToken;
import com.aisc.algoviz.auth.repository.RefreshTokenRepository;
import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.common.exception.CommonErrorCode;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@algoviz.com")
                .password("encoded_pass")
                .fullName("Test User")
                .role("ROLE_USER")
                .points(0)
                .solvedProblemsCount(0)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@algoviz.com")
                .password("secret123")
                .fullName("Test User")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@algoviz.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtils.generateToken("testuser")).thenReturn("access_token_123");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("refresh_token_123");
        when(jwtUtils.getExpirationDateFromJwtToken("refresh_token_123"))
                .thenReturn(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token_123", response.getToken());
        assertEquals("refresh_token_123", response.getRefreshToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw AppException when username already exists")
    void testRegisterUsernameTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@algoviz.com")
                .password("secret123")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(CommonErrorCode.USERNAME_TAKEN, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AppException when email already exists")
    void testRegisterEmailTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("existing@algoviz.com")
                .password("secret123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@algoviz.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(CommonErrorCode.EMAIL_TAKEN, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("secret123")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(jwtUtils.generateToken("testuser")).thenReturn("access_token_123");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("refresh_token_123");
        when(jwtUtils.getExpirationDateFromJwtToken("refresh_token_123"))
                .thenReturn(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token_123", response.getToken());
        assertEquals("refresh_token_123", response.getRefreshToken());
        assertEquals("testuser", response.getUser().getUsername());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login credentials invalid")
    void testLoginInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid password"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should rotate refresh token when refreshing successfully")
    void testRefreshTokenSuccess() {
        String oldRefresh = "valid_old_refresh";
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(oldRefresh)
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .id(10L)
                .token(oldRefresh)
                .user(sampleUser)
                .expiryDate(Instant.now().plus(2, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(jwtUtils.validateJwtToken(oldRefresh)).thenReturn(true);
        when(refreshTokenRepository.findByToken(oldRefresh)).thenReturn(Optional.of(storedToken));
        when(jwtUtils.generateToken("testuser")).thenReturn("new_access_token");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("new_refresh_token");
        when(jwtUtils.getExpirationDateFromJwtToken("new_refresh_token"))
                .thenReturn(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        assertTrue(storedToken.isRevoked(), "Old refresh token must be revoked for rotation security");
    }

    @Test
    @DisplayName("Should reject revoked refresh token")
    void testRefreshTokenAlreadyRevoked() {
        String token = "revoked_token";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();

        RefreshToken storedToken = RefreshToken.builder()
                .id(11L)
                .token(token)
                .user(sampleUser)
                .expiryDate(Instant.now().plus(2, ChronoUnit.DAYS))
                .revoked(true)
                .build();

        when(jwtUtils.validateJwtToken(token)).thenReturn(true);
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(storedToken));

        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(CommonErrorCode.AUTHENTICATION_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should successfully blacklist access token and revoke refresh token on logout")
    void testLogout() {
        String accessToken = "active_access_token";
        String refreshToken = "active_refresh_token";
        Date expDate = Date.from(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(jwtUtils.validateJwtToken(accessToken)).thenReturn(true);
        when(jwtUtils.getExpirationDateFromJwtToken(accessToken)).thenReturn(expDate);

        authService.logout(accessToken, refreshToken);

        verify(tokenBlacklistService, times(1)).blacklistToken(eq(accessToken), eq(expDate.toInstant()));
        verify(refreshTokenRepository, times(1)).revokeByToken(refreshToken);
    }
}
