package com.aisc.algoviz.auth.controller;

import com.aisc.algoviz.auth.dto.AuthResponse;
import com.aisc.algoviz.auth.dto.LoginRequest;
import com.aisc.algoviz.auth.dto.RefreshTokenRequest;
import com.aisc.algoviz.auth.dto.RegisterRequest;
import com.aisc.algoviz.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "APIs đăng ký tài khoản, đăng nhập JWT, làm mới access token và đăng xuất")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản người dùng mới", description = "Tạo tài khoản mới và trả về Access Token cùng Refresh Token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống bằng username hoặc email", description = "Xác thực thông tin và cấp phát Access Token (15 phút) cùng Refresh Token (7 ngày)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới Access Token", description = "Cấp mới Access Token bằng Refresh Token hợp lệ")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Đăng xuất tài khoản",
            description = "Thu hồi Refresh Token và đưa Access Token vào danh sách đen (Blacklist)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok().body(java.util.Map.of("message", "Logged out successfully"));
    }
}
