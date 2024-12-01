package com.buildup.nextQuestion.Controller;
import com.buildup.nextQuestion.Service.QuestionGenerationFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class QuestionController {

    private final QuestionGenerationFacade questionGenerationFacade;

    @PostMapping("api/question/genguest")
    public ResponseEntity<?> uploadFileGuest(
            @RequestParam("file") MultipartFile file,
            @RequestParam("numOfQuestions") int numOfQuestions
    ) {
        try {
            JsonNode jsonNode = questionGenerationFacade.generateQuestionGuest(file, numOfQuestions);
            return ResponseEntity.ok(jsonNode);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error parsing JSON: " + e.getMessage());
        }
    }

//    @PostMapping("api/question/genmember")
//    public ResponseEntity<?> uploadFileMember(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("numOfQuestions") int numOfQuestions
//    ) {
//        try {
//            questionGenerationFacade.generateQuestionMember(file, numOfQuestions);
//            return ResponseEntity.ok(null);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(null);
//        }
//    }
}
