package com.aisc.algoviz.user.controller;

import com.aisc.algoviz.user.dto.UserDto;
import com.aisc.algoviz.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aisc.algoviz.user.dto.UpdateProfileRequest;
import com.aisc.algoviz.user.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import com.aisc.algoviz.user.dto.LearningProgressRequest;
import com.aisc.algoviz.user.service.LearningProgressService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs thông tin hồ sơ cá nhân và cập nhật thông tin người dùng")
public class UserController {

    private final UserService userService;
    private final LearningProgressService learningProgressService;

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
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest body) {
        return ResponseEntity.ok(userService.updateProfile(body));
    }

    @PutMapping("/password")
    @Operation(summary = "Đổi mật khẩu và thu hồi refresh token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        userService.changePassword(body);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/learning-progress")
    @Operation(summary = "Lấy các bài học đã hoàn thành", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Map<String, Object>>> getLearningProgress() {
        return ResponseEntity.ok(learningProgressService.getProgress());
    }

    @PutMapping("/learning-progress")
    @Operation(summary = "Lưu tiến độ bài học", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> saveLearningProgress(@Valid @RequestBody LearningProgressRequest body) {
        learningProgressService.saveProgress(body);
        return ResponseEntity.noContent().build();
    }
}
