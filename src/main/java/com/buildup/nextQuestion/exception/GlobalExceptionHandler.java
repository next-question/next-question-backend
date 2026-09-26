package com.buildup.nextQuestion.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
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
    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class, EntityNotFoundException.class, IOException.class, SecurityException.class})
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request Error", e.getMessage());
    }

    //PDF 추출 자리가 나지 않음 — 서버 오류가 아니라 붐비는 것이므로 재시도 시점을 알려준다
    @ExceptionHandler(PdfExtractionBusyException.class)
    public ResponseEntity<Map<String, Object>> handlePdfExtractionBusy(PdfExtractionBusyException e) {
        ResponseEntity<Map<String, Object>> body = buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", e.getMessage());
        return ResponseEntity.status(body.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(body.getBody());
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
