package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.question.FindQuestionByNormalExamRequest;
import com.buildup.nextQuestion.dto.question.FindQuestionsByNormalExamResponse;
import com.buildup.nextQuestion.dto.workBook.GetQuestionsByWorkBookRequest;
import com.buildup.nextQuestion.dto.workBook.GetQuestionsByWorkBookResponse;
import com.buildup.nextQuestion.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SolvingController {


    private final QuestionService questionService;

    @GetMapping("/solving/normal/search")
    public ResponseEntity<?> searchQuestionsByWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody FindQuestionByNormalExamRequest request
    ){
        try{
            List<FindQuestionsByNormalExamResponse> response = questionService.findQuestionsByNormalExam(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
