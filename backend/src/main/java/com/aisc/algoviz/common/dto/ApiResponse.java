package com.aisc.algoviz.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

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
    private int code;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

}
