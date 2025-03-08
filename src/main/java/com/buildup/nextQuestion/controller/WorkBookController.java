package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.workBook.CreateWorkBookRequest;
import com.buildup.nextQuestion.dto.workBook.CreateWorkBookResponse;
import com.buildup.nextQuestion.dto.workBook.GetWorkBookInfoResponse;
import com.buildup.nextQuestion.dto.workBook.UpdateWorkBookInfoRequest;
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


    @PostMapping("public/workBook/create")
    public ResponseEntity<?> createWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateWorkBookRequest request
    ) throws Exception {
            CreateWorkBookResponse createWorkBookResponse = workBookService.createWorkBook(token, request);
            return ResponseEntity.ok(createWorkBookResponse);
    }

    @GetMapping("public/workBook/search")
    public ResponseEntity<?> getWorkBookInfo(
            @RequestHeader("Authorization") String token
    ) throws Exception {
            List<GetWorkBookInfoResponse> workBookInfos = workBookService.getWorkBookInfo(token);
            return ResponseEntity.ok(workBookInfos);
    }

    @DeleteMapping("public/workBook/delete")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody List<String> encryptedWorkBookInfoIds
    ) throws Exception {
            workBookService.deleteWorkBookInfo(token, encryptedWorkBookInfoIds);
            return ResponseEntity.ok("문제집이 성공적으로 삭제되었습니다.");
    }

    @PatchMapping("public/workBook/update")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateWorkBookInfoRequest updateWorkBookInfoRequest
    ) throws Exception {
            workBookService.updateWorkBookInfo(token, updateWorkBookInfoRequest);
            return ResponseEntity.ok("문제집 명이 성공적으로 변경되었습니다.");
    }

}
