package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.HistoryInfo;
import com.buildup.nextQuestion.dto.follow.QuestionSolveStats;
import com.buildup.nextQuestion.repository.HistoryInfoRepository;
import com.buildup.nextQuestion.repository.HistoryRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class FollowService {

    private final HistoryRepository historyRepository;
    private final HistoryInfoRepository historyInfoRepository;

    public QuestionSolveStats getStatsForQuestion30Days(Long memberId, Long questionId) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        Timestamp ts = Timestamp.valueOf(fromDate);

        List<History> recentHistories = historyRepository.findByMemberIdAndSolvedDateAfter(memberId, ts);

        List<Long> historyIds = recentHistories.stream()
                .map(History::getId)
                .toList();

        List<HistoryInfo> infoList = historyInfoRepository.findByHistoryIdInAndQuestionId(historyIds, questionId);

        int totalAttempts = infoList.size();
        int wrongCount = (int) infoList.stream()
                .filter(info -> Boolean.TRUE.equals(info.getWrong()))
                .count();
        int correctCount = totalAttempts - wrongCount;

        return new QuestionSolveStats(totalAttempts, correctCount, wrongCount);
    }
}