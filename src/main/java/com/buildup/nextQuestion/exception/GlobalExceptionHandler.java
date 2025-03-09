package com.buildup.nextQuestion.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //인증 실패(권한 없음)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException e) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication Required", e.getMessage());
    }

    //잘못된 인자 및 파라미터 전달(아이디 비밀번호 틀리는 경우 등)
    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class, IOException.class, SecurityException.class})
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request Error", e.getMessage());
    }

    //서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        String message = "서버 에러";
        if(e.getMessage() != null){ message = e.getMessage(); }
        else if(e instanceof DataAccessException){ message = "DB 오류";}
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", message);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());

        return ResponseEntity.status(status).body(errorResponse);
    }
}
