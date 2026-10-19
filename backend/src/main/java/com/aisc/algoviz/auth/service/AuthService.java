package com.aisc.algoviz.auth.service;

import com.aisc.algoviz.auth.config.JwtUtils;
import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.common.exception.CommonErrorCode;
import com.aisc.algoviz.auth.dto.*;
import com.aisc.algoviz.user.dto.UserDto;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aisc.algoviz.auth.entity.RefreshToken;
import com.aisc.algoviz.auth.repository.RefreshTokenRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(CommonErrorCode.USERNAME_TAKEN);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(CommonErrorCode.EMAIL_TAKEN);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName() != null ? request.getFullName() : request.getUsername())
                .role("ROLE_USER")
                .points(0)
                .solvedProblemsCount(0)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtUtils.generateToken(savedUser.getUsername());
        String refreshToken = createAndPersistRefreshToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToUserDto(savedUser))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtils.generateToken(user.getUsername());
        String refreshToken = createAndPersistRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenString = request.getRefreshToken();
        if (!jwtUtils.validateJwtToken(tokenString)) {
            throw new AppException(CommonErrorCode.AUTHENTICATION_FAILED);
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new AppException(CommonErrorCode.AUTHENTICATION_FAILED));

        if (storedToken.isRevoked() || storedToken.getExpiryDate().isBefore(Instant.now())) {
            throw new AppException(CommonErrorCode.AUTHENTICATION_FAILED);
        }

        User user = storedToken.getUser();
        // Revoke the old refresh token (refresh token rotation for high security)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtUtils.generateToken(user.getUsername());
        String newRefreshToken = createAndPersistRefreshToken(user);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // 1. Blacklist the access token until its expiration
        if (accessToken != null && jwtUtils.validateJwtToken(accessToken)) {
            Date exp = jwtUtils.getExpirationDateFromJwtToken(accessToken);
            tokenBlacklistService.blacklistToken(accessToken, exp.toInstant());
        }

        // 2. Revoke the refresh token in database
        if (refreshToken != null) {
            refreshTokenRepository.revokeByToken(refreshToken);
        }
    }

    private String createAndPersistRefreshToken(User user) {
        String token = jwtUtils.generateRefreshToken(user.getUsername());
        Date expDate = jwtUtils.getExpirationDateFromJwtToken(token);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expDate.toInstant())
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    public UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .points(user.getPoints())
                .solvedProblemsCount(user.getSolvedProblemsCount())
                .build();
    }
}
