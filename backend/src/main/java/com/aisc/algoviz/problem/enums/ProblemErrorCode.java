package com.aisc.algoviz.problem.enums;


import com.aisc.algoviz.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProblemErrorCode implements ErrorCode {

    INVALID_DIFFICULTY(101,"INVALID DIFFICULTY",HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ProblemErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.status = statusCode;
    }
}
