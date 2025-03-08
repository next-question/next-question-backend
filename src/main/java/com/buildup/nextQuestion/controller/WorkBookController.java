package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.workBook.*;
import com.buildup.nextQuestion.service.WorkBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WorkBookController {

    private final WorkBookService workBookService;


    @PostMapping("member/workBook/create")
    public ResponseEntity<?> createWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateWorkBookRequest request
    ){
        try{
            CreateWorkBookResponse createWorkBookResponse = workBookService.createWorkBook(token, request);
            return ResponseEntity.ok(createWorkBookResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("member/workBooks/search")
    public ResponseEntity<?> searchWorkBook(
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<GetWorkBookResponse> workBookInfos = workBookService.getWorkBook(token);
            return ResponseEntity.ok(workBookInfos);
        } catch (Exception e) {

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "워크북 정보를 가져오는 중 오류 발생" + e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("member/workBook/search/questions")
    public ResponseEntity<?> searchQuestionsByWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody GetQuestionsByWorkBookRequest request
            ){
        try{
            List<GetQuestionsByWorkBookResponse> response = workBookService.searchQuestionsByWorkBook(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("member/workBooks/delete")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody List<String> encryptedWorkBookInfoIds
    ){
        try{
            workBookService.deleteWorkBook(token, encryptedWorkBookInfoIds);
            return ResponseEntity.ok("문제집이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PatchMapping("member/workBook/update")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateWorkBookRequest updateWorkBookRequest
            ){
        try{
            workBookService.updateWorkBook(token, updateWorkBookRequest);
            return ResponseEntity.ok("문제집 명이 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}
