package com.buildup.nextQuestion.performance;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.HistoryInfo;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.QuestionInfo;
import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.domain.enums.SolvedType;
import com.buildup.nextQuestion.repository.HistoryInfoRepository;
import com.buildup.nextQuestion.repository.HistoryRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.repository.WorkBookRepository;
import com.buildup.nextQuestion.repository.projection.SolvedAnswerView;
import com.buildup.nextQuestion.repository.projection.WorkBookQuestionCount;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N+1 개선 전후의 <b>응답 시간</b>을 실제 MySQL 로 잰다.
 *
 * <p>{@link WorkBookListQueryCountTest} 와 {@link StatisticsQueryCountTest} 는 쿼리 수를 센다.
 * 쿼리 수는 어디서 재든 같지만 시간은 다르다. 인메모리 H2 에는 네트워크 라운드트립이 없어
 * 쿼리 하나의 비용이 거의 0 이고, 그래서 개선 폭이 실제보다 작게 나온다.
 *
 * <p><b>실행 방법.</b> MySQL 컨테이너가 떠 있어야 한다.
 * <pre>
 * export BENCH_MYSQL_PASSWORD=아무거나
 * docker run -d --name nq-bench-mysql \
 *   -e MYSQL_ROOT_PASSWORD="$BENCH_MYSQL_PASSWORD" -e MYSQL_DATABASE=nq_bench \
 *   -p 3309:3306 mysql:8.0
 *
 * ./gradlew test --tests "*N1LatencyBenchmarkTest" -Dspring.profiles.active=mysql
 * </pre>
 *
 * <p><b>측정 방법.</b> 시나리오마다 개선 전과 개선 후를 번갈아 돌린다. 워밍업 {@value #WARMUP}
 * 회로 InnoDB 버퍼 풀과 JIT 를 데운 뒤 {@value #ROUNDS} 회를 재고 p50 을 남긴다. 회차마다
 * 영속성 컨텍스트를 비워 1차 캐시가 섞이지 않게 하고, 두 경로가 같은 값을 돌려주는지도
 * 매번 확인한다. 빨라졌지만 결과가 다르면 의미가 없다.
 *
 * <p>개선 전 로직은 서비스 코드에 남아 있지 않으므로 여기에 그대로 재현해 뒀다.
 */
// showSql = false 로 둔다. @DataJpaTest 기본값이 true 라 측정 로그가 SQL 에 묻힌다.
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("mysql")
// mysql 프로파일 없이 돌면 H2 로 떨어져 시간이 실제보다 짧게 나온다. 그 상태로는 아예 건너뛴다.
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*mysql.*")
class N1LatencyBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(N1LatencyBenchmarkTest.class);

    private static final int WARMUP = 2;
    private static final int ROUNDS = 5;

    @Autowired private EntityManager em;
    @Autowired private WorkBookRepository workBookRepository;
    @Autowired private WorkBookInfoRepository workBookInfoRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private HistoryRepository historyRepository;
    @Autowired private HistoryInfoRepository historyInfoRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("문제집 목록 - 10개, 각 20문제")
    void workBookList10() {
        Member member = seedWorkBooks(10, 20);
        em.flush();
        Long id = member.getId();

        Comparison result = compare(
                () -> String.valueOf(legacyCountPerWorkBook(id)),
                () -> String.valueOf(improvedCountPerWorkBook(id)));

        log.info(result.report("문제집 목록 - 10개, 각 20문제"));
        assertThat(result.beforeQueries).isEqualTo(1 + 10 + 10 * 20);
        assertThat(result.afterQueries).isEqualTo(2);
    }

    @Test
    @DisplayName("문제집 목록 - 50개, 각 100문제")
    void workBookList50() {
        Member member = seedWorkBooks(50, 100);
        em.flush();
        Long id = member.getId();

        Comparison result = compare(
                () -> String.valueOf(legacyCountPerWorkBook(id)),
                () -> String.valueOf(improvedCountPerWorkBook(id)));

        log.info(result.report("문제집 목록 - 50개, 각 100문제"));
        assertThat(result.beforeQueries).isEqualTo(1 + 50 + 50 * 100);
        assertThat(result.afterQueries).isEqualTo(2);
    }

    @Test
    @DisplayName("주간 통계 - 6개월치 기록")
    void weeklyStatistics() {
        Member member = seedHistory(180, 3, 10);
        em.flush();
        Long id = member.getId();

        Comparison result = compare(
                () -> String.valueOf(legacyWeeklyStats(id)),
                () -> String.valueOf(improvedWeeklyStats(id)));

        log.info(result.report("주간 통계 - 6개월치 기록"));
        assertThat(result.beforeQueries).isEqualTo(1 + 7 * 3);
        assertThat(result.afterQueries).isEqualTo(1);
    }

    // --- 측정 ---------------------------------------------------------------

    /** 개선 전과 개선 후를 번갈아 돌린다. 측정 도중 머신이 느려져도 한쪽에만 몰리지 않는다. */
    private Comparison compare(Supplier<String> before, Supplier<String> after) {
        for (int i = 0; i < WARMUP; i++) {
            runOnce(before);
            runOnce(after);
        }

        long[] beforeNanos = new long[ROUNDS];
        long[] afterNanos = new long[ROUNDS];
        long beforeQueries = 0, afterQueries = 0;
        String beforeValue = null, afterValue = null;

        for (int i = 0; i < ROUNDS; i++) {
            Measurement b = runOnce(before);
            beforeNanos[i] = b.nanos; beforeQueries = b.queries; beforeValue = b.value;
            Measurement a = runOnce(after);
            afterNanos[i] = a.nanos; afterQueries = a.queries; afterValue = a.value;
        }

        // 빨라진 것만으로는 부족하다. 두 경로가 같은 값을 내야 한다.
        assertThat(afterValue).isEqualTo(beforeValue);
        return new Comparison(beforeNanos, beforeQueries, afterNanos, afterQueries);
    }

    private Measurement runOnce(Supplier<String> path) {
        em.clear();
        statistics.clear();
        long start = System.nanoTime();
        String value = path.get();
        long elapsed = System.nanoTime() - start;
        return new Measurement(elapsed, statistics.getPrepareStatementCount(), value);
    }

    private record Measurement(long nanos, long queries, String value) {}

    private record Comparison(long[] beforeNanos, long beforeQueries,
                              long[] afterNanos, long afterQueries) {

        String report(String title) {
            List<String> lines = new ArrayList<>();
            String bar = "──────────────────────────────────────────────────────────────";
            lines.add("");
            lines.add(bar);
            lines.add(" " + title);
            lines.add(bar);
            lines.add(String.format("  %-10s %10s %12s %12s", "", "쿼리", "p50", "p95"));
            lines.add(String.format("  %-10s %9d회 %12s %12s", "개선 전", beforeQueries,
                    ms(percentile(beforeNanos, 50)), ms(percentile(beforeNanos, 95))));
            lines.add(String.format("  %-10s %9d회 %12s %12s", "개선 후", afterQueries,
                    ms(percentile(afterNanos, 50)), ms(percentile(afterNanos, 95))));
            lines.add(String.format("  %-10s %9s  %12s", "감소",
                    String.format("%.0f배", (double) beforeQueries / afterQueries),
                    String.format("%.1f배", (double) percentile(beforeNanos, 50) / percentile(afterNanos, 50))));
            lines.add(bar);
            lines.add(String.format("  워밍업 %d회 후 %d회 측정. MySQL 8.0, 쿼리 수는 Hibernate 통계의 prepared statement 수.",
                    WARMUP, ROUNDS));
            lines.add(bar);
            return String.join(System.lineSeparator(), lines);
        }

        private static String ms(long nanos) {
            double millis = nanos / 1_000_000.0;
            return millis >= 1000 ? String.format("%.2f s", millis / 1000) : String.format("%.1f ms", millis);
        }

        private static long percentile(long[] values, int p) {
            long[] sorted = values.clone();
            Arrays.sort(sorted);
            int index = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
        }
    }

    // --- 개선 전후 로직 (서비스 코드에 남아 있지 않은 쪽은 재현) -----------------

    private Map<Long, Long> legacyCountPerWorkBook(Long memberId) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (WorkBook workBook : workBookRepository.findAllByMemberId(memberId)) {
            long total = 0;
            for (WorkBookInfo info : workBookInfoRepository.findAllByWorkBookId(workBook.getId())) {
                Question question = questionRepository
                        .findByMemberIdAndQuestionInfoId(memberId, info.getQuestionInfo().getId())
                        .orElseThrow();
                if (!question.getDel()) total++;
            }
            counts.put(workBook.getId(), total);
        }
        return counts;
    }

    private Map<Long, Long> improvedCountPerWorkBook(Long memberId) {
        List<WorkBook> workBooks = workBookRepository.findAllByMemberId(memberId);
        List<Long> ids = workBooks.stream().map(WorkBook::getId).toList();
        Map<Long, Long> counted = workBookInfoRepository
                .countActiveQuestionsByWorkBookIds(memberId, ids).stream()
                .collect(Collectors.toMap(WorkBookQuestionCount::getWorkBookId,
                        WorkBookQuestionCount::getQuestionCount));
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (WorkBook workBook : workBooks) {
            counts.put(workBook.getId(), counted.getOrDefault(workBook.getId(), 0L));
        }
        return counts;
    }

    private Map<LocalDate, List<Integer>> legacyWeeklyStats(Long memberId) {
        Map<LocalDate, List<Integer>> statsMap = emptyWeek();
        for (History history : historyRepository.findAllByMemberId(memberId)) {
            if (history.getSolvedDate() == null) continue;
            List<Integer> stats = statsMap.get(history.getSolvedDate().toLocalDateTime().toLocalDate());
            if (stats == null) continue;
            for (HistoryInfo info : historyInfoRepository.findAllByHistoryId(history.getId())) {
                stats.set(0, stats.get(0) + 1);
                if (Boolean.FALSE.equals(info.getWrong())) stats.set(1, stats.get(1) + 1);
            }
        }
        return statsMap;
    }

    private Map<LocalDate, List<Integer>> improvedWeeklyStats(Long memberId) {
        Map<LocalDate, List<Integer>> statsMap = emptyWeek();
        LocalDate today = LocalDate.now();
        for (SolvedAnswerView answer : historyInfoRepository.findSolvedAnswersBetween(
                memberId,
                Timestamp.valueOf(today.minusDays(6).atStartOfDay()),
                Timestamp.valueOf(today.plusDays(1).atStartOfDay()))) {
            if (answer.getSolvedDate() == null) continue;
            List<Integer> stats = statsMap.get(answer.getSolvedDate().toLocalDateTime().toLocalDate());
            if (stats == null) continue;
            stats.set(0, stats.get(0) + 1);
            if (Boolean.FALSE.equals(answer.getWrong())) stats.set(1, stats.get(1) + 1);
        }
        return statsMap;
    }

    private Map<LocalDate, List<Integer>> emptyWeek() {
        Map<LocalDate, List<Integer>> statsMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) statsMap.put(today.minusDays(i), new ArrayList<>(List.of(0, 0)));
        return statsMap;
    }

    // --- 데이터 준비 ---------------------------------------------------------

    private Member seedWorkBooks(int workBookCount, int questionsPerWorkBook) {
        Member member = new Member("tester", LoginType.LOCAL);
        em.persist(member);
        for (int w = 0; w < workBookCount; w++) {
            WorkBook workBook = new WorkBook();
            workBook.setMember(member);
            workBook.setName("문제집 " + w);
            workBook.setRecentSolveDate(new Timestamp(System.currentTimeMillis()));
            em.persist(workBook);
            for (int q = 0; q < questionsPerWorkBook; q++) {
                QuestionInfo questionInfo = new QuestionInfo();
                questionInfo.setName("문제 " + w + "-" + q);
                questionInfo.setType(QuestionType.OX);
                questionInfo.setAnswer("O");
                em.persist(questionInfo);
                em.persist(new Question(member, questionInfo));
                em.persist(new WorkBookInfo(questionInfo, workBook));
            }
        }
        return member;
    }

    private Member seedHistory(int days, int sessionsPerDay, int questionsPerSession) {
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
}
