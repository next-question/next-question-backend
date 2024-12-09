package com.buildup.nextQuestion.Controller;
import com.buildup.nextQuestion.Service.QuestionGenerationFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@AllArgsConstructor
public class QuestionController {

    private final QuestionGenerationFacade questionGenerationFacade;
    private final ObjectMapper objectMapper;

    @PostMapping("api/file/upload/guest")
    public ResponseEntity<?> uploadFileByGuest(
            @RequestPart("file") MultipartFile File,
            @RequestPart("jsonFile") MultipartFile jsonFile
    ) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonFile.getInputStream());
            int numOfQuestions = rootNode.path("option").path("numOfQuestions").asInt();

            JsonNode jsonNode = questionGenerationFacade.generateQuestionByGuest(File, numOfQuestions);

            return ResponseEntity.ok(jsonNode);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error parsing JSON: " + e.getMessage());
        }
    }

    @PostMapping("api/file/upload/member")
    public ResponseEntity<?> uploadFileByMember(
            @RequestPart("file") MultipartFile File,
            @RequestPart("jsonFile") MultipartFile jsonFile
    ) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonFile.getInputStream());
            int numOfQuestions = rootNode.path("option").path("numOfQuestions").asInt();

            questionGenerationFacade.generateQuestionByMember(File, numOfQuestions);
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

}
