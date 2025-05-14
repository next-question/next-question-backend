package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.question.MemberQuestionInfoDto;
import com.buildup.nextQuestion.dto.statistics.DayQuestionStats;
import com.buildup.nextQuestion.dto.statistics.QuestionStatisticsRequest;
import com.buildup.nextQuestion.service.SolvingService;
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

    // 1. 푼 문제 목록 조회
    @GetMapping("/solved")
    public ResponseEntity<List<MemberQuestionInfoDto>> getSolvedQuestions(
            @RequestHeader("Authorization") String token,
            @RequestBody QuestionStatisticsRequest request) {

        List<MemberQuestionInfoDto> result = statisticsService.getSolvedQuestion(token, request);
        return ResponseEntity.ok(result);
    }

    // 2. 틀린 문제 목록 조회
    @GetMapping("/wrong")
    public ResponseEntity<List<MemberQuestionInfoDto>> getWrongQuestions(
            @RequestHeader("Authorization") String token,
            @RequestBody QuestionStatisticsRequest request) {

        List<MemberQuestionInfoDto> result = statisticsService.getWrongQuestion(token, request);
        return ResponseEntity.ok(result);
    }

    // 3. 자주 틀린 문제 목록 조회
    @GetMapping("/wrong-frequent")
    public ResponseEntity<List<MemberQuestionInfoDto>> getFrequentlyWrongQuestions(
            @RequestHeader("Authorization") String token,
            @RequestBody QuestionStatisticsRequest request) {

        List<MemberQuestionInfoDto> result = statisticsService.getFrequentlyWrongQuestion(token, request);
        return ResponseEntity.ok(result);
    }
}
