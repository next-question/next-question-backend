package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.domain.enums.SolvedType;
import com.buildup.nextQuestion.dto.question.MemberQuestionInfoResponse;
import com.buildup.nextQuestion.dto.solving.ExamInfoDTO;
import com.buildup.nextQuestion.dto.solving.SaveHistoryByExamRequest;
import com.buildup.nextQuestion.dto.solving.WorkBookInfoDTO;
import com.buildup.nextQuestion.dto.statistics.*;
import com.buildup.nextQuestion.mapper.QuestionMapper;
import com.buildup.nextQuestion.repository.HistoryInfoRepository;
import com.buildup.nextQuestion.repository.HistoryRepository;
import com.buildup.nextQuestion.repository.StatisticsRepository;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
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
    private final StatisticsRepository statisticsRepository;

    public List<DayQuestionStats> findCorrectQuestions(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        List<History> histories = historyRepository.findAllByMemberId(member.getId());

        // 요일 한글 매핑
        Map<DayOfWeek, String> dayOfWeekKorean = Map.of(
                DayOfWeek.MONDAY, "월",
                DayOfWeek.TUESDAY, "화",
                DayOfWeek.WEDNESDAY, "수",
                DayOfWeek.THURSDAY, "목",
                DayOfWeek.FRIDAY, "금",
                DayOfWeek.SATURDAY, "토",
                DayOfWeek.SUNDAY, "일"
        );

        // 최근 7일 날짜
        LocalDate today = LocalDate.now();
        Map<LocalDate, DayQuestionStats> statsMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String koreanDay = dayOfWeekKorean.get(date.getDayOfWeek());

            DayQuestionStats stats = new DayQuestionStats();
            stats.setDay(koreanDay);
            statsMap.put(date, stats);
        }

        for (History history : histories) {
            if (history.getSolvedDate() == null) continue;

            LocalDate solveDate = history.getSolvedDate().toLocalDateTime().toLocalDate();

            if (statsMap.containsKey(solveDate)) {
                DayQuestionStats stats = statsMap.get(solveDate);
                List<HistoryInfo> historyInfos = historyInfoRepository.findAllByHistoryId(history.getId());

                for (HistoryInfo historyInfo : historyInfos) {
                    stats.setTotal(stats.getTotal() + 1);
                    if (Boolean.FALSE.equals(historyInfo.getWrong())) {
                        stats.setCorrect(stats.getCorrect() + 1);
                    }
                }
            }
        }

        return new ArrayList<>(statsMap.values());
    }

    @Transactional
    public void generateTestHistoryData(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        Random random = new Random();
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= 6; i++) {
            LocalDate date = today.minusDays(i);
            int totalQuestions = random.nextInt(30) + 1; // 1~30개 랜덤 문제 수

            // History 생성
            History history = new History();
            history.setMember(member);
            history.setSolvedDate(Timestamp.valueOf(date.atStartOfDay())); // 해당 날짜 00시
            historyRepository.save(history);

            // 각 문제에 대해 HistoryInfo 생성
            for (int j = 0; j < totalQuestions; j++) {
                HistoryInfo info = new HistoryInfo();
                info.setHistory(history);

                // 70% 확률로 맞춘 것으로 설정
                boolean isCorrect = random.nextDouble() < 0.7;
                info.setWrong(!isCorrect);

                historyInfoRepository.save(info);
            }
        }
    }



    // 특정 멤버의 몇 일 이내의 푼 문제 HistoryInfo 리스트 반환 (중복 O)
    private List<HistoryInfo> getSolvedHistoryInfosBetween(Long memberId, LocalDateTime from, LocalDateTime to) {
        Timestamp fromTimestamp = Timestamp.valueOf(from);
        Timestamp toTimestamp = Timestamp.valueOf(to);

        List<SolvedType> types = List.of(
                SolvedType.NORMAL,
                SolvedType.MOCK,
                SolvedType.WRONG
        );

        List<History> histories = historyRepository.findByMemberIdAndSolvedDateBetweenAndTypeIn(
                memberId,
                fromTimestamp,
                toTimestamp,
                types
        );

        List<Long> historyIds = histories.stream()
                .map(History::getId)
                .toList();

        if (historyIds.isEmpty()) return List.of();

        return historyInfoRepository.findByHistoryIdIn(historyIds);
    }

    // 몇일 이내에 푼 문제 (중복X)
    public List<MemberQuestionInfoResponse> getSolvedQuestion(String token, QuestionStatisticsRequest request) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        LocalDateTime from = LocalDate.now().minusDays(request.getDays()).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        List<HistoryInfo> historyInfos = getSolvedHistoryInfosBetween(memberId, from, to);

        List<Question> questions = historyInfos.stream()
                .map(HistoryInfo::getQuestion)
                .distinct()
                .toList();

        return questionMapper.memberQuestionInfoMapper(questions);
    }

    // 몇일 이내에 틀린 문제 (중복X)
    public List<MemberQuestionInfoResponse> getWrongQuestion(String token, QuestionStatisticsRequest request) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        LocalDateTime from = LocalDate.now().minusDays(request.getDays()).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        List<HistoryInfo> historyInfos = getSolvedHistoryInfosBetween(memberId, from, to);

        List<Question> questions = historyInfos.stream()
                .filter(info -> Boolean.TRUE.equals(info.getWrong()))
                .map(HistoryInfo::getQuestion)
                .distinct()
                .toList();

        return questionMapper.memberQuestionInfoMapper(questions);
    }

    // 몇일 이내에 n회 이상 틀린 문제 (중복X)
    public List<MemberQuestionInfoResponse> getFrequentlyWrongQuestion(String token, QuestionStatisticsRequest request) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        int threshold = request.getThreshold() != null ? request.getThreshold() : 3;

        LocalDateTime from = LocalDate.now().minusDays(request.getDays()).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        List<HistoryInfo> historyInfos = getSolvedHistoryInfosBetween(memberId, from, to);

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

    public ProfileStatisticResponse getProfileStatistics(String token) {
                String userId = jwtUtility.getUserIdFromToken(token);
                Member member = memberFinder.findMember(userId);
        return new ProfileStatisticResponse(
                member.getNickname(),
                userId,
                getAverageCorrectRate(token).getAverageCorrectRate(),
                getTodaySolvedCount(token).getSolvedNum(),
                getThisMonthSolvedCount(token).getSolvedNum(),
                getMonthlyAverageSolvedCount(token).getAverageSolvedNum(),
                getStreak(token).getStreak(),
                getMaxStreak(token).getStreak(),
                getDailySolveCountThisMonth(token)
        );
    }

    public AverageCorrectRateDto getAverageCorrectRate(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        Statistics stats = statisticsRepository.findByMember(member)
            .orElseThrow(() -> new IllegalStateException("통계 정보가 없습니다."));

        int total = stats.getTotalAttempts();
        int correct = stats.getCorrectCount();

        int roundedCorrectRate = 0;
        if (total > 0) {
            double averageCorrectRate = ((double) correct / total) * 100;
            roundedCorrectRate = (int) Math.round(averageCorrectRate);
        }

        return new AverageCorrectRateDto(roundedCorrectRate);
    }

    public SolvedNumDto getTodaySolvedCount(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<HistoryInfo> todayHistoryInfos = getSolvedHistoryInfosBetween(memberId, startOfToday, now);

        return new SolvedNumDto(todayHistoryInfos.size());
    }

    public SolvedNumDto getThisMonthSolvedCount(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<HistoryInfo> monthHistoryInfos = getSolvedHistoryInfosBetween(memberId, startOfMonth, now);

        return new SolvedNumDto(monthHistoryInfos.size());
    }

    public AverageSolvedNumDto getMonthlyAverageSolvedCount(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        // 이번 달의 시작부터 현재까지 범위
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<HistoryInfo> historyInfos = getSolvedHistoryInfosBetween(memberId, startOfMonth, now);

        int totalSolved = historyInfos.size();
        int daysPassed = Period.between(startOfMonth.toLocalDate(), now.toLocalDate()).getDays() + 1;

        double rawAverage = daysPassed > 0 ? (double) totalSolved / daysPassed : 0.0;

        // 소수점 1자리 반올림
        double roundedAverage = Math.round(rawAverage * 10.0) / 10.0;

        return new AverageSolvedNumDto(roundedAverage);
    }

    public StreakDto getStreak(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        Statistics stats = statisticsRepository.findByMember(member)
                .orElseThrow(() -> new IllegalStateException("통계 정보가 없습니다."));

        return new StreakDto(stats.getStreak());
    }

    public StreakDto getMaxStreak(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        Statistics stats = statisticsRepository.findByMember(member)
                .orElseThrow(() -> new IllegalStateException("통계 정보가 없습니다."));

        return new StreakDto(stats.getMaxStreak());
    }

    public List<DailySolveCountDto> getDailySolveCountThisMonth(String token) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);
        Long memberId = member.getId();

        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.withDayOfMonth(1);
        LocalDate lastDay = now.withDayOfMonth(now.lengthOfMonth());

        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = lastDay.atTime(LocalTime.MAX);

        List<HistoryInfo> historyInfos = getSolvedHistoryInfosBetween(memberId, from, to);

        Map<LocalDate, Long> countsPerDay = historyInfos.stream()
                .collect(Collectors.groupingBy(
                        info -> info.getHistory().getSolvedDate().toLocalDateTime().toLocalDate(),
                        Collectors.counting()
                ));

        List<DailySolveCountDto> result = new ArrayList<>();
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            int count = countsPerDay.getOrDefault(date, 0L).intValue();
            result.add(new DailySolveCountDto(date, count));
        }

        return result;
    }

    public void initStatistics(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("회원가입 통계 초기화에 실패했습니다: member가 없습니다.");
        }

        Statistics stats = new Statistics();
        stats.setMember(member);
        stats.setStreak(0);
        stats.setMaxStreak(0);
        stats.setTotalAttempts(0);
        stats.setCorrectCount(0);
        statisticsRepository.save(stats);
    }

    @Transactional
    public void updateTotalAtmAndCorrectCnt(String token, SaveHistoryByExamRequest request) {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        int totalAttempts = 0;
        int correctCount = 0;

        for (WorkBookInfoDTO workBookInfoDTO : request.getWorkBookInfoDTOS()) {
            for (ExamInfoDTO info : workBookInfoDTO.getInfo()) {
                totalAttempts++;
                if (!info.getWrong()) correctCount++;
            }
        }

        Statistics stats = statisticsRepository.findByMember(member)
                .orElseThrow(() -> new EntityNotFoundException("통계가 존재하지 않습니다."));

        stats.setTotalAttempts(stats.getTotalAttempts() + totalAttempts);
        stats.setCorrectCount(stats.getCorrectCount() + correctCount);

        statisticsRepository.save(stats);
    }









}
