package com.aisc.algoviz.user.service;

import com.aisc.algoviz.auth.service.AuthService;
import com.aisc.algoviz.auth.repository.RefreshTokenRepository;
import com.aisc.algoviz.common.exception.AppException;
import com.aisc.algoviz.common.exception.CommonErrorCode;
import com.aisc.algoviz.user.dto.UpdateProfileRequest;
import com.aisc.algoviz.user.dto.ChangePasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.aisc.algoviz.user.dto.UserDto;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    public UserDto getCurrentUserProfile() {
        return authService.mapToUserDto(getCurrentUser());
    }

    public UserDto updateProfile(String fullName, String avatarUrl) {
        User user = getCurrentUser();
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
        }
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }
        User updated = userRepository.save(user);
        return authService.mapToUserDto(updated);
    }

    @Transactional
    public UserDto updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            userRepository.findByEmail(email).filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> { throw new AppException(CommonErrorCode.EMAIL_TAKEN); });
            user.setEmail(email);
        }
        if (request.getFullName() != null) user.setFullName(request.getFullName().trim());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl().trim());
        if (request.getRoleTitle() != null) user.setRoleTitle(request.getRoleTitle().trim());
        if (request.getBio() != null) user.setBio(request.getBio().trim());
        if (request.getLocation() != null) user.setLocation(request.getLocation().trim());
        if (request.getTarget() != null) user.setTarget(request.getTarget().trim());
        return authService.mapToUserDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(CommonErrorCode.INVALID_CURRENT_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(user);
    }
}
