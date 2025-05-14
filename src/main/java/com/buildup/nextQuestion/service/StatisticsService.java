package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.HistoryInfo;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.dto.statistics.DayQuestionStats;
import com.buildup.nextQuestion.repository.HistoryInfoRepository;
import com.buildup.nextQuestion.repository.HistoryRepository;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    private final JwtUtility jwtUtility;
    private final MemberFinder memberFinder;
    private final HistoryInfoRepository historyInfoRepository;
    private final HistoryRepository historyRepository;

    public List<DayQuestionStats> findCorrectQuestions(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        List<History> histories = historyRepository.findAllByMemberId(member.getId());

        List<String> weekDays = List.of("월", "화", "수", "목", "금", "토", "일");
        List<DayQuestionStats> statsList = new ArrayList<>();

        for (String day : weekDays) {
            DayQuestionStats stats = new DayQuestionStats();
            stats.setDay(day);
            statsList.add(stats);
        }

        for (History history : histories) {
            if (history.getSolvedDate() == null) continue;

            LocalDate solveDate = history.getSolvedDate().toLocalDateTime().toLocalDate();
            int dayValue = solveDate.getDayOfWeek().getValue(); // 월=1, 일=7

            DayQuestionStats stats = statsList.get(dayValue - 1);

            List<HistoryInfo> historyInfos = historyInfoRepository.findAllByHistoryId(history.getId());

            for (HistoryInfo historyInfo : historyInfos) {
                stats.setTotal(stats.getTotal() + 1);

                // 틀렸는지 여부 확인 (wrong = true → 틀림 / false → 맞음)
                if (Boolean.FALSE.equals(historyInfo.getWrong())) {
                    stats.setCorrect(stats.getCorrect() + 1);
                }
            }
        }

        return statsList;
    }

}
