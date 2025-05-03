package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.statistics.DayQuestionStats;
import com.buildup.nextQuestion.service.SolvingService;
import com.buildup.nextQuestion.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
