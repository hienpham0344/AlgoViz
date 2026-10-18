package com.aisc.algoviz.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonErrorCode implements ErrorCode {

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
