package com.buildup.nextQuestion.performance;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.HistoryInfo;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.QuestionInfo;
import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.domain.enums.SolvedType;
import com.buildup.nextQuestion.repository.HistoryInfoRepository;
import com.buildup.nextQuestion.repository.HistoryRepository;
import com.buildup.nextQuestion.repository.projection.SolvedAnswerView;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주간 학습 통계 조회의 쿼리 수와 읽어오는 행 수를 개선 전/후로 비교한다.
 *
 * <p>개선 전에는 회원의 학습 기록을 <b>전 기간</b> 가져온 뒤 최근 7일치만 골라내고,
 * 남은 기록마다 그 안의 문제들을 다시 조회했다. 기록이 쌓일수록 두 가지가 같이 나빠진다 —
 * 쿼리 수(기록 수에 비례)와 읽어오는 행 수(전 기간).
 *
 * <p>개선 후에는 기간 조건을 DB로 내리고 필요한 두 값(푼 날짜·오답 여부)만 조인해서 한 번에 읽는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StatisticsQueryCountTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private HistoryInfoRepository historyInfoRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("6개월치 기록이 쌓인 회원 — 개선 전 22쿼리, 개선 후 1쿼리")
    void sixMonthsOfHistory() {
        // 180일 동안 하루 3번, 매번 10문제씩 풀었다고 가정
        Member member = seed(180, 3, 10);
        em.flush();
        em.clear();

        statistics.clear();
        long legacyStart = System.nanoTime();
        Map<LocalDate, List<Integer>> legacy = legacyWeeklyStats(member.getId());
        long legacyNanos = System.nanoTime() - legacyStart;
        long legacyQueries = statistics.getPrepareStatementCount();

        em.clear();

        statistics.clear();
        long improvedStart = System.nanoTime();
        Map<LocalDate, List<Integer>> improved = improvedWeeklyStats(member.getId());
        long improvedNanos = System.nanoTime() - improvedStart;
        long improvedQueries = statistics.getPrepareStatementCount();

        System.out.println(report("6개월(180일) · 하루 3회 학습 · 회당 10문제",
                legacyQueries, legacyNanos, improvedQueries, improvedNanos));

        assertThat(improved).isEqualTo(legacy);
        // 개선 전: 기록 전체 조회 1 + 최근 7일 기록마다 1회씩
        assertThat(legacyQueries).isEqualTo(1 + 7 * 3);
        // 개선 후: 조인 조회 1회
        assertThat(improvedQueries).isEqualTo(1);
    }

    @Test
    @DisplayName("개선 전은 전 기간을 읽고, 개선 후는 7일치만 읽는다")
    void improvedReadsOnlyTheWeek() {
        Member member = seed(180, 3, 10);
        em.flush();
        em.clear();

        // 개선 전 첫 쿼리가 가져오는 행 수 = 회원의 모든 학습 기록
        int legacyFirstQueryRows = historyRepository.findAllByMemberId(member.getId()).size();
        em.clear();

        // 개선 후 쿼리가 가져오는 행 수 = 최근 7일간 푼 문제 수
        LocalDate today = LocalDate.now();
        int improvedRows = historyInfoRepository.findSolvedAnswersBetween(
                member.getId(),
                Timestamp.valueOf(today.minusDays(6).atStartOfDay()),
                Timestamp.valueOf(today.plusDays(1).atStartOfDay())).size();

        System.out.println(String.format(
                "%n  개선 전 첫 쿼리가 읽는 학습 기록 행: %d개 (전 기간)%n"
                        + "  개선 후 쿼리가 읽는 행: %d개 (최근 7일)%n",
                legacyFirstQueryRows, improvedRows));

        assertThat(legacyFirstQueryRows).isEqualTo(180 * 3);
        assertThat(improvedRows).isEqualTo(7 * 3 * 10);
    }

    // --- 개선 전/후 로직 ------------------------------------------------------

    /** 개선 전 — 기록 전체를 가져와 메모리에서 7일치를 고르고, 기록마다 다시 조회한다. */
    private Map<LocalDate, List<Integer>> legacyWeeklyStats(Long memberId) {
        Map<LocalDate, List<Integer>> statsMap = emptyWeek();

        List<History> histories = historyRepository.findAllByMemberId(memberId);
        for (History history : histories) {
            if (history.getSolvedDate() == null) continue;
            LocalDate solvedDate = history.getSolvedDate().toLocalDateTime().toLocalDate();

            List<Integer> stats = statsMap.get(solvedDate);
            if (stats == null) continue;

            for (HistoryInfo info : historyInfoRepository.findAllByHistoryId(history.getId())) {
                stats.set(0, stats.get(0) + 1);
                if (Boolean.FALSE.equals(info.getWrong())) {
                    stats.set(1, stats.get(1) + 1);
                }
            }
        }
        return statsMap;
    }

    /** 개선 후 — 기간 조건을 DB 로 내리고 필요한 두 값만 한 번에 읽는다. */
    private Map<LocalDate, List<Integer>> improvedWeeklyStats(Long memberId) {
        Map<LocalDate, List<Integer>> statsMap = emptyWeek();

        LocalDate today = LocalDate.now();
        List<SolvedAnswerView> answers = historyInfoRepository.findSolvedAnswersBetween(
                memberId,
                Timestamp.valueOf(today.minusDays(6).atStartOfDay()),
                Timestamp.valueOf(today.plusDays(1).atStartOfDay()));

        for (SolvedAnswerView answer : answers) {
            if (answer.getSolvedDate() == null) continue;
            List<Integer> stats = statsMap.get(answer.getSolvedDate().toLocalDateTime().toLocalDate());
            if (stats == null) continue;

            stats.set(0, stats.get(0) + 1);
            if (Boolean.FALSE.equals(answer.getWrong())) {
                stats.set(1, stats.get(1) + 1);
            }
        }
        return statsMap;
    }

    private Map<LocalDate, List<Integer>> emptyWeek() {
        Map<LocalDate, List<Integer>> statsMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            statsMap.put(today.minusDays(i), new ArrayList<>(List.of(0, 0)));
        }
        return statsMap;
    }

    // --- 데이터 준비 ---------------------------------------------------------

    private Member seed(int days, int sessionsPerDay, int questionsPerSession) {
        Member member = new Member("tester", LoginType.LOCAL);
        em.persist(member);

        LocalDate today = LocalDate.now();
        for (int d = 0; d < days; d++) {
          for (int session = 0; session < sessionsPerDay; session++) {
            History history = new History();
            history.setMember(member);
            history.setSolvedDate(Timestamp.valueOf(
                    today.minusDays(d).atStartOfDay().plusHours(9 + session)));
            history.setType(SolvedType.NORMAL);
            em.persist(history);

            for (int q = 0; q < questionsPerSession; q++) {
                QuestionInfo questionInfo = new QuestionInfo();
                questionInfo.setName("문제 " + d + "-" + q);
                questionInfo.setType(QuestionType.OX);
                questionInfo.setAnswer("O");
                em.persist(questionInfo);

                Question question = new Question(member, questionInfo);
                em.persist(question);

                HistoryInfo info = new HistoryInfo();
                info.setHistory(history);
                info.setQuestion(question);
                info.setWrong(q % 3 == 0);
                em.persist(info);
            }
          }
        }
        return member;
    }

    // --- 리포트 -------------------------------------------------------------

    private static String report(String title, long legacyQueries, long legacyNanos,
                                 long improvedQueries, long improvedNanos) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("─────────────────────────────────────────────");
        lines.add(" 주간 학습 통계 — " + title);
        lines.add("─────────────────────────────────────────────");
        lines.add(String.format("  개선 전  쿼리 %4d회   %.1fms", legacyQueries, legacyNanos / 1_000_000.0));
        lines.add(String.format("  개선 후  쿼리 %4d회   %.1fms", improvedQueries, improvedNanos / 1_000_000.0));
        lines.add("─────────────────────────────────────────────");
        return String.join(System.lineSeparator(), lines);
    }
}
