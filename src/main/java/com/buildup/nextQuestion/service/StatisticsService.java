package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.HistoryInfo;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.enums.SolvedType;
import com.buildup.nextQuestion.dto.question.MemberQuestionInfoDto;
import com.buildup.nextQuestion.dto.statistics.DayQuestionStats;
import com.buildup.nextQuestion.dto.statistics.QuestionStatisticsRequest;
import com.buildup.nextQuestion.mapper.QuestionMapper;
import com.buildup.nextQuestion.repository.HistoryInfoRepository;
import com.buildup.nextQuestion.repository.HistoryRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    private final JwtUtility jwtUtility;
    private final MemberFinder memberFinder;
    private final HistoryInfoRepository historyInfoRepository;
    private final HistoryRepository historyRepository;
    private final QuestionMapper questionMapper;

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

    // 특정 멤버의 몇 일 이내의 푼 문제 HistoryInfo 리스트 반환 (중복 O)
    private List<HistoryInfo> getSolvedHistoryInfos(Long memberId, int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        Timestamp timestamp = Timestamp.valueOf(fromDate);

        List<History> recentHistories = historyRepository.findByMemberIdAndSolvedDateAfterAndTypeIn(
                memberId,
                timestamp,
                List.of(SolvedType.NORMAL, SolvedType.MOCK)
        );

        List<Long> historyIds = recentHistories.stream()
                .map(History::getId)
                .toList();

        if (historyIds.isEmpty()) return List.of();

        return historyInfoRepository.findByHistoryIdIn(historyIds);
    }

    // 몇일 이내에 푼 문제 (중복X)
    public List<MemberQuestionInfoDto> getSolvedQuestion(String token, QuestionStatisticsRequest request) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        List<HistoryInfo> historyInfos = getSolvedHistoryInfos(memberId, request.getDays());

        List<Question> questions = historyInfos.stream()
                .map(HistoryInfo::getQuestion)
                .distinct()
                .toList();

        return questionMapper.memberQuestionInfoMapper(questions);
    }
    // 몇일 이내에 틀린 문제 (중복X)
    public List<MemberQuestionInfoDto> getWrongQuestion(String token, QuestionStatisticsRequest request) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        List<HistoryInfo> historyInfos = getSolvedHistoryInfos(memberId, request.getDays());

        List<Question> questions = historyInfos.stream()
                .filter(info -> Boolean.TRUE.equals(info.getWrong()))
                .map(HistoryInfo::getQuestion)
                .distinct()
                .toList();

        return questionMapper.memberQuestionInfoMapper(questions);
    }

    // 몇일 이내에 n회 이상 틀린 문제 (중복X)
    public List<MemberQuestionInfoDto> getFrequentlyWrongQuestion(String token, QuestionStatisticsRequest request) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        int threshold = request.getThreshold() != null ? request.getThreshold() : 3;

        List<HistoryInfo> historyInfos = getSolvedHistoryInfos(memberId, request.getDays());

        List<Question> questions = historyInfos.stream()
                .filter(info -> Boolean.TRUE.equals(info.getWrong()))
                .collect(Collectors.groupingBy(
                        info -> info.getQuestion().getId(),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() >= threshold)
                .map(entry -> entry.getValue().get(0).getQuestion())
                .toList();

        return questionMapper.memberQuestionInfoMapper(questions);
    }



}
