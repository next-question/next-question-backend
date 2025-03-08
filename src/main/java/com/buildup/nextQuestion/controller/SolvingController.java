package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.question.FindQuestionByNormalExamRequest;
import com.buildup.nextQuestion.dto.question.FindQuestionsByNormalExamResponse;
import com.buildup.nextQuestion.dto.solving.SaveHistoryByNormalExamRequest;
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
    public ResponseEntity<?> searchQuestionsByWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody FindQuestionByNormalExamRequest request
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
}
