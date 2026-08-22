package com.buildup.nextQuestion.controller;
import com.buildup.nextQuestion.dto.question.*;
import com.buildup.nextQuestion.dto.solving.FindQuestionsByTypeResponse;
import com.buildup.nextQuestion.service.FileService;
import com.buildup.nextQuestion.service.QuestionGenerationFacade;
import com.buildup.nextQuestion.service.QuestionService;
import com.buildup.nextQuestion.service.question.GenerationJob;
import com.buildup.nextQuestion.service.question.QuestionGenerationJobService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationFacade questionGenerationFacade;
    private final QuestionService questionService;
    private final FileService fileService;
    private final QuestionGenerationJobService questionGenerationJobService;


    @PostMapping("public/questions/upload")
    public ResponseEntity<?> uploadFileByGuest(@ModelAttribute UploadFileByGuestRequest request) throws IOException {
            fileService.validateFile(request.getFile());

            JsonNode response = questionGenerationFacade.generateQuestionByGuest(request);
            return ResponseEntity.ok(response);
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

    /**
     * 문제 생성을 백그라운드로 맡기고 작업 id 만 즉시 돌려준다.
     *
     * <p>기존 동기 엔드포인트({@code member/questions/upload})는 그대로 둔다 — 이미 붙어 있는
     * 클라이언트를 깨뜨리지 않기 위해서다. 새로 붙는 쪽은 이 경로를 쓰면 생성이 오래 걸려도
     * 요청이 묶이지 않는다.
     *
     * @return 202 Accepted 와 작업 id. 대기열이 가득 차면 503.
     */
    @PostMapping("member/questions/upload/async")
    public ResponseEntity<?> uploadFileByMemberAsync(
            @RequestHeader("Authorization") String token,
            @ModelAttribute UploadFileByMemberRequest request
    ) throws IOException {
        fileService.validateFile(request.getFile());

        try {
            GenerationJob job = questionGenerationFacade.generateQuestionByMemberAsync(request);
            return ResponseEntity.accepted().body(QuestionGenerationJobResponse.from(job));
        } catch (RejectedExecutionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "지금은 생성 요청이 밀려 있습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    /** 비동기 생성 작업의 진행 상태를 확인한다. */
    @GetMapping("member/questions/upload/async/{jobId}")
    public ResponseEntity<?> findGenerationJob(
            @RequestHeader("Authorization") String token,
            @PathVariable String jobId
    ) {
        return questionGenerationJobService.find(jobId)
                .<ResponseEntity<?>>map(job -> ResponseEntity.ok(QuestionGenerationJobResponse.from(job)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "해당 생성 작업을 찾을 수 없습니다.")));
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
            @RequestBody DeleteQuestionRequest request
            ) throws Exception
    {
        FindQuestionsByTypeResponse response = questionService.deleteQuestion(token, request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("member/questions/move")
    public ResponseEntity<?> deleteQuestion(
            @RequestHeader("Authorization") String token,
            @RequestBody MoveQuestionRequest request
    ) throws Exception
    {
        List<FindQuestionsByTypeResponse> responses = questionService.moveQuestion(token, request);
        return ResponseEntity.ok(responses);
    }

}