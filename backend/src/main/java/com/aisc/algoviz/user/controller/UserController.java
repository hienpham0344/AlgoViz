package com.aisc.algoviz.user.controller;

import com.aisc.algoviz.user.dto.UserDto;
import com.aisc.algoviz.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody Map<String, String> body) {
        String fullName = body.get("fullName");
        String avatarUrl = body.get("avatarUrl");
        return ResponseEntity.ok(userService.updateProfile(fullName, avatarUrl));
    }
}
