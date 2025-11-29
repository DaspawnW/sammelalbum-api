package com.daspawnw.sammelalbum.exception;

import com.daspawnw.sammelalbum.dto.AuthDtos.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(AuthResponse.builder()
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<AuthResponse> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(403).body(AuthResponse.builder()
                .message(ex.getMessage())
                .build());
    }
}
