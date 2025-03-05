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
    ){
        try{
            CreateWorkBookResponse createWorkBookResponse = workBookService.createWorkBook(token, request);
            return ResponseEntity.ok(createWorkBookResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("public/workBook/search")
    public ResponseEntity<?> getWorkBookInfo(
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<GetWorkBookInfoResponse> workBookInfos = workBookService.getWorkBookInfo(token);
            return ResponseEntity.ok(workBookInfos);
        } catch (Exception e) {

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "워크북 정보를 가져오는 중 오류 발생" + e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @DeleteMapping("public/workBook/delete")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody List<String> encryptedWorkBookInfoIds
    ){
        try{
            workBookService.deleteWorkBookInfo(token, encryptedWorkBookInfoIds);
            return ResponseEntity.ok("문제집이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PatchMapping("public/workBook/update")
    public ResponseEntity<String> deleteWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateWorkBookInfoRequest updateWorkBookInfoRequest
            ){
        try{
            workBookService.updateWorkBookInfo(token, updateWorkBookInfoRequest);
            return ResponseEntity.ok("문제집 명이 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}
