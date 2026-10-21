package com.aisc.algoviz.user.controller;

import com.aisc.algoviz.user.dto.UserDto;
import com.aisc.algoviz.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs thông tin hồ sơ cá nhân và cập nhật thông tin người dùng")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "Lấy hồ sơ cá nhân của người dùng hiện tại",
            description = "Trả về thông tin chi tiết: username, email, điểm thưởng (points), số bài đã giải và streak",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PutMapping("/profile")
    @Operation(
            summary = "Cập nhật hồ sơ cá nhân",
            description = "Cập nhật họ tên hiển thị và URL avatar của người dùng",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserDto> updateProfile(@RequestBody Map<String, String> body) {
        String fullName = body.get("fullName");
        String avatarUrl = body.get("avatarUrl");
        return ResponseEntity.ok(userService.updateProfile(fullName, avatarUrl));
    }
}
