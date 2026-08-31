package com.aisc.algoviz.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    int getCode();
    String getMessage();
    HttpStatus getStatus();
}
