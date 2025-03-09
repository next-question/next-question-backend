package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.solving.*;
import com.buildup.nextQuestion.service.SolvingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SolvingController {


    private final SolvingService solvingService;

    @GetMapping("/solving/normal/search")
    public ResponseEntity<?> searchQuestionsByNormalExam(
            @RequestHeader("Authorization") String token,
            @RequestBody FindQuestionsByNormalExamRequest request
    ){
        try{
            List<FindQuestionsByNormalExamResponse> response = solvingService.findQuestionsByNormalExam(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/solving/normal/save")
    public ResponseEntity<?> saveHistoryByNormalExam(
            @RequestHeader("Authorization") String token,
            @RequestBody SaveHistoryByNormalExamRequest request
    ){
        try{
            solvingService.saveHistoryByNormalExam(token, request);
            return ResponseEntity.ok("문제 풀이 결과를 저장했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/solving/mock/search")
    public ResponseEntity<?> searchQuestionsByMockExam(
            @RequestHeader("Authorization") String token,
            @RequestBody FindQuestionsByMockExamRequest request
    ){
        try{
            List<FindQuestionsByMockExamResponse> response = solvingService.findQuestionsByMockExam(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @GetMapping("/solving/histories/search")
    public ResponseEntity<?> saveHistoryByNormalExam(
            @RequestHeader("Authorization") String token
    ){
        try{
            List<FindHistoryByMemberResponse> response = solvingService.findHistoryByMember(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/solving/historyInfo/search")
    public ResponseEntity<?> findHistoryInfoByHistory(
            @RequestHeader("Authorization") String token,
            @RequestBody FindHistoryInfoByHistoryRequest request
    ){
        try{
            List<FindHistoryInfoByHistoryResponse> response = solvingService.findHistoryInfoByHistory(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
