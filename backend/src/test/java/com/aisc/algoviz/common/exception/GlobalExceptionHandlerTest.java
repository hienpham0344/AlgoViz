package com.aisc.algoviz.common.exception;

import com.aisc.algoviz.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleAppException should return ResponseEntity matching ErrorCode status and payload")
    void testHandleAppException() {
        AppException ex = new AppException(CommonErrorCode.AUTHENTICATION_FAILED);

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAppException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(4010, response.getBody().getCode());
        assertEquals("Invalid username/email or password", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleAuthenticationException should translate to AUTHENTICATION_FAILED")
    void testHandleAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Bad creds");

        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAuthenticationException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(4010, response.getBody().getCode());
    }

    @Test
    @DisplayName("handleValidationExceptions should return 400 with field errors mapped in data")
    void testHandleValidationExceptions() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError("userDto", "email", "must be a well-formed email address");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(bindingResult.getGlobalErrors()).thenReturn(List.of());
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(CommonErrorCode.INVALID_REQUEST.getCode(), response.getBody().getCode());
        assertNotNull(response.getBody().getData());
        assertEquals("must be a well-formed email address", response.getBody().getData().get("email"));
    }

    @Test
    @DisplayName("handleConstraintViolationException should return 400 with details")
    void testHandleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Value too low");

        ConstraintViolationException ex = new ConstraintViolationException("Constraint error", Set.of(violation));

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleConstraintViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(CommonErrorCode.INVALID_REQUEST.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("handleIllegalArgumentException should return 400 Bad Request")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument passed");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(CommonErrorCode.INVALID_REQUEST.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("handleHttpMessageNotReadable should return 400 MALFORMED_JSON")
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Cannot parse JSON");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(CommonErrorCode.MALFORMED_JSON.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch should return 400 INVALID_PARAMETER")
    void testHandleMethodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("problemId");
        when(ex.getValue()).thenReturn("not-a-number");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(CommonErrorCode.INVALID_PARAMETER.getCode(), response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("problemId"));
    }

    @Test
    @DisplayName("handleGeneralException should return 500 INTERNAL_SERVER_ERROR")
    void testHandleGeneralException() {
        RuntimeException ex = new RuntimeException("Unexpected fatal failure");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().getCode());
        assertEquals(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(), response.getBody().getMessage());
    }
}
