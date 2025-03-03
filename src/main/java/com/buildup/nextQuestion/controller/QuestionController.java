package com.buildup.nextQuestion.controller;
import com.buildup.nextQuestion.service.QuestionGenerationFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class QuestionController {

    private final QuestionGenerationFacade questionGenerationFacade;
    private final ObjectMapper objectMapper;

    @PostMapping("guest/file/upload/")
    public ResponseEntity<?> uploadFileByGuest(
            @RequestPart("files") MultipartFile[] files
    ) {
        try {
            MultipartFile pdfFile = null;
            MultipartFile jsonFile = null;

            for (MultipartFile file : files) {
                if (file.getOriginalFilename().endsWith(".pdf")) {
                    pdfFile = file;
                } else if (file.getOriginalFilename().endsWith(".json")) {
                    jsonFile = file;
                }
            }
            if (pdfFile == null || jsonFile == null) {
                return ResponseEntity.badRequest().body("Both PDF and JSON files are required.");
            }

            JsonNode rootNode = objectMapper.readTree(jsonFile.getInputStream());
            int numOfQuestions = rootNode.path("option").path("numOfQuestions").asInt();

            JsonNode jsonNode = questionGenerationFacade.generateQuestionByGuest(pdfFile, numOfQuestions);
            return ResponseEntity.ok(jsonNode);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing files: " + e.getMessage());
        }
    }

    @PostMapping("member/file/upload")
    public ResponseEntity<?> uploadFileByMember(
            @RequestPart("files") MultipartFile[] files
    ) {
        try {
            MultipartFile pdfFile = null;
            MultipartFile jsonFile = null;

            for (MultipartFile file : files) {
                if (file.getOriginalFilename().endsWith(".pdf")) {
                    pdfFile = file;
                } else if (file.getOriginalFilename().endsWith(".json")) {
                    jsonFile = file;
                }
            }
            if (pdfFile == null || jsonFile == null) {
                return ResponseEntity.badRequest().body("Both PDF and JSON files are required.");
            }

            JsonNode rootNode = objectMapper.readTree(jsonFile.getInputStream());
            int numOfQuestions = rootNode.path("option").path("numOfQuestions").asInt();

            questionGenerationFacade.generateQuestionByMember(pdfFile, numOfQuestions);
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing files: " + e.getMessage());
        }
    }

}
