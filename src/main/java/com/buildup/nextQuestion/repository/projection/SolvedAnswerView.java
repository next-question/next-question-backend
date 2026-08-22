package com.buildup.nextQuestion.repository.projection;

import java.sql.Timestamp;

/**
 * 푼 문제 한 건 — 언제 풀었고 틀렸는지만 담는다.
 *
 * <p>주간 통계는 날짜별 "푼 수 / 맞힌 수"만 필요하다. 학습 기록과 그 안의 문제들을
 * 엔티티째로 끌어오면 기록 수만큼 쿼리가 늘어나므로, 필요한 두 값만 한 번에 읽는다.
 */
public interface SolvedAnswerView {

    Timestamp getSolvedDate();

    Boolean getWrong();
}
