package com.buildup.nextQuestion.controller;
import com.buildup.nextQuestion.dto.question.*;
import com.buildup.nextQuestion.service.FileService;
import com.buildup.nextQuestion.service.QuestionGenerationFacade;
import com.buildup.nextQuestion.service.QuestionService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationFacade questionGenerationFacade;
    private final QuestionService questionService;
    private final FileService fileService;


    @PostMapping("public/questions/upload")
    public ResponseEntity<?> uploadFileByGuest(@ModelAttribute UploadFileByGuestRequest request) throws IOException {
            fileService.validateFile(request.getFile());

            JsonNode jsonNode = questionGenerationFacade.generateQuestionByGuest(request);
            return ResponseEntity.ok(jsonNode);
    }

    @PostMapping("member/questions/upload")
    public ResponseEntity<?> uploadFileByMember(
            @RequestHeader("Authorization") String token,
            @ModelAttribute UploadFileByMemberRequest request
            ) throws Exception {
            MultipartFile pdfFile = request.getFile();

            fileService.validateFile(pdfFile);

        List<UploadFileByMemberResponse> response = questionGenerationFacade.generateQuestionByMember(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("member/questions/save")
    public ResponseEntity<?> saveQuestion(
            @RequestHeader("Authorization") String token,
            @RequestBody SaveQuestionRequest saveQuestionRequest) throws Exception {

        questionService.saveQuestion(token, saveQuestionRequest);
        return ResponseEntity.ok("문제를 성공적으로 저장했습니다.");
    }

    @GetMapping("member/questions/search")
    public ResponseEntity<?> saveQuestion(
            @RequestHeader("Authorization") String token) throws Exception
    {
            List<FindQuestionByMemberResponse> response = questionService.findQuestionByMember(token);
            return ResponseEntity.ok(response);
    }

    @DeleteMapping("member/questions/delete")
    public ResponseEntity<?> deleteQuestion(
            @RequestHeader("Authorization") String token,
            @RequestBody List<String> encryptedQuestionInfoIds
            ) throws Exception
    {
            questionService.deleteQuestion(token, encryptedQuestionInfoIds);
            return ResponseEntity.ok("문제가 성공적으로 삭제되었습니다.");
    }


    @PostMapping("member/questions/move")
    public ResponseEntity<?> deleteQuestion(
            @RequestHeader("Authorization") String token,
            @RequestBody MoveQuestionRequest request
    ) throws Exception
    {
        questionService.moveQuestion(token, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "문제를 성공적으로 이동했습니다."
        ));
    }

}