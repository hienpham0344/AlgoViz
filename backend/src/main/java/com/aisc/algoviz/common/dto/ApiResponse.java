package com.aisc.algoviz.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lớp vỏ bọc (Response Envelope) chuẩn hóa toàn bộ phản hồi từ API.
 * Đảm bảo mọi kết quả trả về cho Frontend luôn tuân theo cấu trúc thống nhất:
 * {
 *   "status": 200,
 *   "message": "Thành công",
 *   "data": { ... },
 *   "timestamp": "2026-08-27T17:40:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Phương thức tiện ích tạo nhanh phản hồi thành công (HTTP 200 OK)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Phương thức tiện ích tạo nhanh phản hồi thành công kèm thông điệp tùy chỉnh
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Phương thức tiện ích tạo nhanh phản hồi lỗi
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
