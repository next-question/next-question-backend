package com.buildup.nextQuestion.controller;
import com.buildup.nextQuestion.dto.member.LoginRequest;
import com.buildup.nextQuestion.dto.member.LoginResponse;
import com.buildup.nextQuestion.dto.question.SaveQuestionRequest;
import com.buildup.nextQuestion.dto.question.SearchQuestionByMemberResponse;
import com.buildup.nextQuestion.dto.question.UploadFileByMemberReqeust;
import com.buildup.nextQuestion.service.QuestionGenerationFacade;
import com.buildup.nextQuestion.service.QuestionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationFacade questionGenerationFacade;
    private final QuestionService questionService;

    @PostMapping("public/question/upload")
    public ResponseEntity<?> uploadFileByGuest(
            @RequestPart MultipartFile file
    ) {
        try {
            if (file == null) {
                return ResponseEntity.badRequest().body("PDF file is required.");
            }

            JsonNode jsonNode = questionGenerationFacade.generateQuestionByGuest(file);
            return ResponseEntity.ok(jsonNode);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing files: " + e.getMessage());
        }
    }

    @PostMapping("member/question/upload")
    public ResponseEntity<?> uploadFileByMember(
            @RequestHeader("Authorization") String token,
            @ModelAttribute UploadFileByMemberReqeust uploadFileByMemberReqeust
            ) {
        try {
            MultipartFile pdfFile = uploadFileByMemberReqeust.getFile();

            if (pdfFile == null) {
                return ResponseEntity.badRequest().body("PDF file is required.");
            }

            List<String> encryptedQeustionIds = questionGenerationFacade.generateQuestionByMember(pdfFile, uploadFileByMemberReqeust.getNumOfQuestions());

            return ResponseEntity.ok(encryptedQeustionIds);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing files: " + e.getMessage());
        }
    }

    @PostMapping("member/question/save")
    public ResponseEntity<?> saveQuestion(
            @RequestHeader("Authorization") String token,
            @RequestBody SaveQuestionRequest saveQuestionRequest) {
        try {
            questionService.saveQuestion(token, saveQuestionRequest);
            return ResponseEntity.ok("문제를 성공적으로 저장했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다." + e.getMessage());
        }
    }

    @GetMapping("member/question/search")
    public ResponseEntity<?> saveQuestion(
            @RequestHeader("Authorization") String token)
    {
        try {
            List<SearchQuestionByMemberResponse> response = questionService.searchQuestionByMember(token);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다." + e.getMessage());
        }
    }

    @PostMapping("member/question/delete")
    public ResponseEntity<?> deleteQuestion(
            @RequestHeader("Authorization") String token,
            @RequestBody List<String> encryptedQuestionInfoIds
            )
    {
        try {
            questionService.deleteQuestion(token, encryptedQuestionInfoIds);
            return ResponseEntity.ok("문제가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다." + e.getMessage());
        }
    }

}
