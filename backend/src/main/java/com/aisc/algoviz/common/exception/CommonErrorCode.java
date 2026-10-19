package com.aisc.algoviz.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonErrorCode implements ErrorCode {

    AUTHENTICATION_FAILED(4010, "Invalid username/email or password", HttpStatus.UNAUTHORIZED),
    USERNAME_TAKEN(4090, "Username is already taken", HttpStatus.CONFLICT),
    EMAIL_TAKEN(4091, "Email is already in use", HttpStatus.CONFLICT),

    INVALID_REQUEST(
            4000,
            "Dữ liệu đầu vào không hợp lệ",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_SORT_PROPERTY(
            4001,
            "Trường sắp xếp không hợp lệ",
            HttpStatus.BAD_REQUEST
    ),

    MALFORMED_JSON(
            4002,
            "Dữ liệu JSON gửi lên sai định dạng hoặc không thể đọc được",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_PARAMETER(
            4003,
            "Tham số không đúng định dạng",
            HttpStatus.BAD_REQUEST
    ),

    TOO_MANY_REQUESTS(
            4290,
            "Quá nhiều yêu cầu gửi lên hệ thống. Vui lòng thử lại sau.",
            HttpStatus.TOO_MANY_REQUESTS
    ),

    INTERNAL_SERVER_ERROR(
            5000,
            "Lỗi hệ thống nội bộ, vui lòng thử lại sau",
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final int code;
    private final String message;
    private final HttpStatus status;

    CommonErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
