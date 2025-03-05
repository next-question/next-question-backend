package com.buildup.nextQuestion.controller;
import com.buildup.nextQuestion.dto.question.UploadFileByMemberReqeust;
import com.buildup.nextQuestion.service.QuestionGenerationFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationFacade questionGenerationFacade;

    @PostMapping("public/file/upload")
    public ResponseEntity<?> uploadFileByGuest(
            @RequestPart MultipartFile pdfFile
    ) {
        try {
            if (pdfFile == null) {
                return ResponseEntity.badRequest().body("PDF file is required.");
            }

            JsonNode jsonNode = questionGenerationFacade.generateQuestionByGuest(pdfFile);
            return ResponseEntity.ok(jsonNode);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing files: " + e.getMessage());
        }
    }

    @PostMapping("member/file/upload")
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

}
