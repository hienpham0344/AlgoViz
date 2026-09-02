package com.aisc.algoviz.problem.exception;

import com.aisc.algoviz.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProblemErrorCode implements ErrorCode {

    INVALID_DIFFICULTY(
            4001,
            "Mức độ khó của bài toán không hợp lệ (Chỉ chấp nhận EASY, MEDIUM, HARD)",
            HttpStatus.BAD_REQUEST
    ),

    PROBLEM_NOT_FOUND(
            4040,
            "Không tìm thấy bài toán trong hệ thống",
            HttpStatus.NOT_FOUND
    ),

    SLUG_ALREADY_EXISTS(
            4004,
            "Bài toán với slug này đã tồn tại trong hệ thống",
            HttpStatus.BAD_REQUEST
    ),

    SOLUTION_NOT_FOUND(
            4041,
            "Không tìm thấy lời giải tham khảo cho bài toán này",
            HttpStatus.NOT_FOUND
    );

    private final int code;
    private final String message;
    private final HttpStatus status;

    ProblemErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
