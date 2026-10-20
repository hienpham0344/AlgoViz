package com.aisc.algoviz.user.service;

import com.aisc.algoviz.auth.service.AuthService;
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
}
