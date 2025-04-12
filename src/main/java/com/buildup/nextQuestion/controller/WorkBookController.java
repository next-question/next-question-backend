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
    ) throws Exception {
            CreateWorkBookResponse createWorkBookResponse = workBookService.createWorkBook(token, request);
            return ResponseEntity.ok(createWorkBookResponse);
    }

    @PostMapping("member/workBooks/search")
    public ResponseEntity<?> searchWorkBook(
            @RequestHeader("Authorization") String token
    ) throws Exception {
            List<GetWorkBookResponse> workBookInfos = workBookService.getWorkBook(token);
            return ResponseEntity.ok(workBookInfos);
    }


    @PostMapping("member/workBook/search/questions")
    public ResponseEntity<?> searchQuestionsByWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody GetQuestionsByWorkBookRequest request
    ) throws Exception {
            List<GetQuestionsByWorkBookResponse> response = workBookService.searchQuestionsByWorkBook(token, request);
            return ResponseEntity.ok(response);
    }

    @DeleteMapping("member/workBooks/delete")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody List<String> encryptedWorkBookInfoIds

    ) throws Exception{
            workBookService.deleteWorkBook(token, encryptedWorkBookInfoIds);
            return ResponseEntity.ok("문제집이 성공적으로 삭제되었습니다.");
    }

    @PatchMapping("member/workBook/update")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateWorkBookRequest updateWorkBookRequest
            ) throws Exception{
            workBookService.updateWorkBook(token, updateWorkBookRequest);
            return ResponseEntity.ok("문제집 명이 성공적으로 변경되었습니다.");
    }
}
