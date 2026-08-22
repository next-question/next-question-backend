package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.HistoryInfo;
import com.buildup.nextQuestion.repository.projection.SolvedAnswerView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public interface HistoryInfoRepository extends JpaRepository<HistoryInfo, Long> {
    List<HistoryInfo> findAllByHistoryId(Long historyId);

    /**
     * 기간 안에 푼 문제들을 "푼 날짜 + 오답 여부"만으로 한 번에 읽는다.
     *
     * <p>주간 통계가 쓰던 방식은 회원의 학습 기록을 전부 가져온 뒤 최근 7일치만 골라내고,
     * 기록마다 그 안의 문제들을 다시 조회했다. 기간 조건을 DB로 내리고 조인해서 한 번에
     * 읽도록 바꾼 것이 이 쿼리다.
     */
    @Query("""
            select h.solvedDate as solvedDate, hi.wrong as wrong
            from HistoryInfo hi
              join hi.history h
            where h.member.id = :memberId
              and h.solvedDate >= :from
              and h.solvedDate < :to
            """)
    List<SolvedAnswerView> findSolvedAnswersBetween(@Param("memberId") Long memberId,
                                                    @Param("from") Timestamp from,
                                                    @Param("to") Timestamp to);
    List<HistoryInfo> findByWrongIsTrueAndHistoryIn(List<History> histories);
    List<HistoryInfo> findByWrongIsTrueAndHistoryIdIn(List<Long> historyIds);
    List<HistoryInfo> findByWrongIsTrueAndHistoryId(Long historyId);
    List<HistoryInfo> findByHistory(History history);
    List<HistoryInfo> findByHistoryIdIn(List<Long> historyIds);

    /**
     * 푼 기록과 함께 문제·문제 내용까지 한 번에 읽는다.
     *
     * <p>기간별 "푼 문제 / 틀린 문제" 목록은 결과를 만들 때 문제와 그 내용을 반드시 꺼내 쓴다.
     * 지연 로딩에 맡기면 기록 수만큼, 그리고 문제 수만큼 쿼리가 더 나간다.
     *
     * <p>문제가 연결되지 않은 기록도 있을 수 있어 left join 을 쓴다 — 걸러내는 것은 호출부 몫이다.
     */
    @Query("""
            select hi from HistoryInfo hi
              left join fetch hi.question q
              left join fetch q.questionInfo
            where hi.history.id in :historyIds
            """)
    List<HistoryInfo> findByHistoryIdInWithQuestion(@Param("historyIds") List<Long> historyIds);
    List<HistoryInfo> findByHistoryIdInAndQuestionId(List<Long> historyIds, Long questionId);

}
