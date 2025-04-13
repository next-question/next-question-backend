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

    @GetMapping("member/solving/daily/search")
    public ResponseEntity<?> searchDailyQuestions(
            @RequestHeader("Authorization") String token
    ) throws Exception {
        List<GetOrAssignTodayQuestionsResponse> response = solvingService.getOrAssignTodayQuestions(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("member/solving/daily/check")
    public ResponseEntity<?> recordAttendance(
            @RequestHeader("Authorization") String token,
            @RequestBody List<RecordAttendanceRequest> requests
    ) throws Exception {
        solvingService.recordAttendance(token,requests);
        return ResponseEntity.ok("출석 체크 되었습니다.");
    }


    @PostMapping("member/solving/normal/search")
    public ResponseEntity<?> searchQuestionsByNormalExam(
            @RequestHeader("Authorization") String token,
            @RequestBody FindQuestionsByNormalExamRequest request
    ) throws Exception {
            List<FindQuestionsByNormalExamResponse> response = solvingService.findQuestionsByNormalExam(token, request);
            return ResponseEntity.ok(response);
    }

    @PostMapping("member/solving/save")
    public ResponseEntity<?> saveHistoryByExam(
            @RequestHeader("Authorization") String token,
            @RequestBody SaveHistoryByExamRequest request
    )throws Exception{
            solvingService.saveHistoryByExam(token, request);
            return ResponseEntity.ok("문제 풀이 결과를 저장했습니다.");
    }

    @PostMapping("member/solving/mock/search")
    public ResponseEntity<?> searchQuestionsByMockExam(
            @RequestHeader("Authorization") String token,
            @RequestBody FindQuestionsByMockExamRequest request
    )throws Exception{
            List<FindQuestionsByMockExamResponse> response = solvingService.findQuestionsByMockExam(token, request);
            return ResponseEntity.ok(response);
    }


    @GetMapping("member/solving/histories/search")
    public ResponseEntity<?> saveHistoryByNormalExam(
            @RequestHeader("Authorization") String token
    ) throws Exception {
            List<FindHistoryByMemberResponse> response = solvingService.findHistoryByMember(token);
            return ResponseEntity.ok(response);
    }


    @PostMapping("member/solving/normal/search/type")
    public ResponseEntity<?> findHistoryInfoByHistory(
            @RequestHeader("Authorization") String token,
            @RequestBody List<String> request
    ) throws Exception {
        FindQuestionsByTypeResponse response = solvingService.findQuestionsByTypeResponse(token, request);
        return ResponseEntity.ok(response);
    }


}
