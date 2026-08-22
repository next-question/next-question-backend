package com.buildup.nextQuestion.performance;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.QuestionInfo;
import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.repository.WorkBookRepository;
import com.buildup.nextQuestion.repository.projection.WorkBookQuestionCount;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 문제집 목록 조회의 쿼리 수를 개선 전/후로 나란히 잰다.
 *
 * <p>개선 전 로직은 {@link #legacyCountPerWorkBook}에 그대로 재현해 뒀다. 실제 서비스 코드는
 * 개선된 쪽으로 바뀌었지만, 두 방식이 같은 결과를 낸다는 것과 쿼리 수가 얼마나 차이 나는지를
 * 언제든 다시 확인할 수 있어야 하기 때문이다.
 *
 * <p>쿼리 수는 Hibernate 통계의 prepared statement 수로 센다 — 실제로 DB에 나간 SQL 개수다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WorkBookListQueryCountTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private WorkBookRepository workBookRepository;

    @Autowired
    private WorkBookInfoRepository workBookInfoRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("문제집 10개 × 문제 20개 — 개선 전 211쿼리, 개선 후 2쿼리")
    void workBooks10_questions20() {
        Result result = measure(10, 20);

        System.out.println(result.report("문제집 10개 × 문제 20개"));

        assertThat(result.legacyCounts).isEqualTo(result.improvedCounts);
        assertThat(result.legacyQueries).isEqualTo(1 + 10 + 10 * 20);
        assertThat(result.improvedQueries).isEqualTo(2);
    }

    @Test
    @DisplayName("문제집 50개 × 문제 100개 — 개선 전 5051쿼리, 개선 후 2쿼리")
    void workBooks50_questions100() {
        Result result = measure(50, 100);

        System.out.println(result.report("문제집 50개 × 문제 100개"));

        assertThat(result.legacyCounts).isEqualTo(result.improvedCounts);
        assertThat(result.legacyQueries).isEqualTo(1 + 50 + 50 * 100);
        assertThat(result.improvedQueries).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제된 문제는 두 방식 모두 세지 않는다")
    void deletedQuestionsAreExcludedInBothPaths() {
        Member member = seed(3, 10);

        // 첫 문제집의 문제 4개를 삭제 처리
        List<WorkBook> workBooks = workBookRepository.findAllByMemberId(member.getId());
        List<WorkBookInfo> infos = workBookInfoRepository.findAllByWorkBookId(workBooks.get(0).getId());
        for (int i = 0; i < 4; i++) {
            Long questionInfoId = infos.get(i).getQuestionInfo().getId();
            questionRepository.findByMemberIdAndQuestionInfoId(member.getId(), questionInfoId)
                    .orElseThrow()
                    .setDel(true);
        }
        em.flush();
        em.clear();

        Map<Long, Long> legacy = legacyCountPerWorkBook(member.getId());
        Map<Long, Long> improved = improvedCountPerWorkBook(member.getId());

        assertThat(legacy).isEqualTo(improved);
        assertThat(legacy.get(workBooks.get(0).getId())).isEqualTo(6L);
    }

    // --- 측정 ---------------------------------------------------------------

    private Result measure(int workBookCount, int questionsPerWorkBook) {
        Member member = seed(workBookCount, questionsPerWorkBook);
        em.flush();
        em.clear();

        statistics.clear();
        long legacyStart = System.nanoTime();
        Map<Long, Long> legacyCounts = legacyCountPerWorkBook(member.getId());
        long legacyNanos = System.nanoTime() - legacyStart;
        long legacyQueries = statistics.getPrepareStatementCount();

        em.clear();

        statistics.clear();
        long improvedStart = System.nanoTime();
        Map<Long, Long> improvedCounts = improvedCountPerWorkBook(member.getId());
        long improvedNanos = System.nanoTime() - improvedStart;
        long improvedQueries = statistics.getPrepareStatementCount();

        return new Result(workBookCount, questionsPerWorkBook,
                legacyCounts, legacyQueries, legacyNanos,
                improvedCounts, improvedQueries, improvedNanos);
    }

    /**
     * 개선 전 로직 — WorkBookService.getWorkBook 이 하던 그대로.
     * 문제집마다 구성 목록을 조회하고, 그 안에서 문제를 하나씩 조회해 삭제 여부를 확인한다.
     */
    private Map<Long, Long> legacyCountPerWorkBook(Long memberId) {
        Map<Long, Long> counts = new LinkedHashMap<>();

        List<WorkBook> workBooks = workBookRepository.findAllByMemberId(memberId);
        for (WorkBook workBook : workBooks) {
            long total = 0;
            for (WorkBookInfo workBookInfo : workBookInfoRepository.findAllByWorkBookId(workBook.getId())) {
                Long questionInfoId = workBookInfo.getQuestionInfo().getId();
                Question question = questionRepository
                        .findByMemberIdAndQuestionInfoId(memberId, questionInfoId)
                        .orElseThrow();
                if (!question.getDel()) {
                    total++;
                }
            }
            counts.put(workBook.getId(), total);
        }
        return counts;
    }

    /** 개선 후 로직 — 문제집 목록 1회 + 집계 1회. */
    private Map<Long, Long> improvedCountPerWorkBook(Long memberId) {
        List<WorkBook> workBooks = workBookRepository.findAllByMemberId(memberId);
        List<Long> workBookIds = workBooks.stream().map(WorkBook::getId).toList();

        Map<Long, Long> counted = workBookInfoRepository
                .countActiveQuestionsByWorkBookIds(memberId, workBookIds)
                .stream()
                .collect(Collectors.toMap(WorkBookQuestionCount::getWorkBookId,
                        WorkBookQuestionCount::getQuestionCount));

        Map<Long, Long> counts = new LinkedHashMap<>();
        for (WorkBook workBook : workBooks) {
            counts.put(workBook.getId(), counted.getOrDefault(workBook.getId(), 0L));
        }
        return counts;
    }

    // --- 데이터 준비 ---------------------------------------------------------

    private Member seed(int workBookCount, int questionsPerWorkBook) {
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

    // --- 리포트 -------------------------------------------------------------

    private record Result(int workBookCount, int questionsPerWorkBook,
                          Map<Long, Long> legacyCounts, long legacyQueries, long legacyNanos,
                          Map<Long, Long> improvedCounts, long improvedQueries, long improvedNanos) {

        String report(String title) {
            Function<Long, String> ms = nanos -> String.format("%.1fms", nanos / 1_000_000.0);
            List<String> lines = new ArrayList<>();
            lines.add("");
            lines.add("─────────────────────────────────────────────");
            lines.add(" " + title);
            lines.add("─────────────────────────────────────────────");
            lines.add(String.format("  개선 전  쿼리 %5d회   %s", legacyQueries, ms.apply(legacyNanos)));
            lines.add(String.format("  개선 후  쿼리 %5d회   %s", improvedQueries, ms.apply(improvedNanos)));
            lines.add(String.format("  감소     %.2f%%  (%d → %d, %.0f배)",
                    (1 - (double) improvedQueries / legacyQueries) * 100, legacyQueries, improvedQueries,
                    (double) legacyNanos / improvedNanos));
            lines.add("─────────────────────────────────────────────");
            return String.join(System.lineSeparator(), lines);
        }
    }
}
