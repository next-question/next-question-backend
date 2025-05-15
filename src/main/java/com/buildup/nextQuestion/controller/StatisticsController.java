package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.question.MemberQuestionInfoResponse;
import com.buildup.nextQuestion.dto.statistics.DayQuestionStats;
import com.buildup.nextQuestion.dto.statistics.ProfileStatisticResponse;
import com.buildup.nextQuestion.dto.statistics.QuestionStatisticsRequest;
import com.buildup.nextQuestion.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member/statistics/")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("main")
    public ResponseEntity<List<DayQuestionStats>> getStatsByDay(
            @RequestHeader("Authorization") String token
    ) {
        List<DayQuestionStats> stats = statisticsService.findCorrectQuestions(token);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("main/test-data")
    public ResponseEntity<?> getTestStatsByDay(
            @RequestHeader("Authorization") String token
    ) {
        statisticsService.generateTestHistoryData(token);
        return ResponseEntity.ok("테스트 데이터 생성 완료");
    }

    // 1. 푼 문제 목록 조회
    @GetMapping("/solved")
    public ResponseEntity<List<MemberQuestionInfoResponse>> getSolvedQuestions(
            @RequestHeader("Authorization") String token,
            @RequestBody QuestionStatisticsRequest request) {

        List<MemberQuestionInfoResponse> result = statisticsService.getSolvedQuestion(token, request);
        return ResponseEntity.ok(result);
    }

    // 2. 틀린 문제 목록 조회
    @GetMapping("/wrong")
    public ResponseEntity<List<MemberQuestionInfoResponse>> getWrongQuestions(
            @RequestHeader("Authorization") String token,
            @RequestBody QuestionStatisticsRequest request) {

        List<MemberQuestionInfoResponse> result = statisticsService.getWrongQuestion(token, request);
        return ResponseEntity.ok(result);
    }

    // 3. 자주 틀린 문제 목록 조회
    @GetMapping("/wrong-frequent")
    public ResponseEntity<List<MemberQuestionInfoResponse>> getFrequentlyWrongQuestions(
            @RequestHeader("Authorization") String token,
            @RequestBody QuestionStatisticsRequest request) {

        List<MemberQuestionInfoResponse> result = statisticsService.getFrequentlyWrongQuestion(token, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileStatisticResponse> getProfileStatistics(
            @RequestHeader("Authorization") String token) {

        ProfileStatisticResponse profileStatisticRespones = statisticsService.getProfileStatistics(token);
        return ResponseEntity.ok(profileStatisticRespones);
    }
}
