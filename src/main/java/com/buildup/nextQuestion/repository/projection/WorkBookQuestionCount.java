package com.buildup.nextQuestion.repository.projection;

/**
 * 문제집별 "살아있는 문제 수" 집계 결과.
 *
 * <p>문제집 목록 화면은 문제집마다 삭제되지 않은 문제가 몇 개인지만 필요하다.
 * 문제를 하나씩 조회해 세면 문제 수만큼 쿼리가 늘어나므로, 집계는 DB에서 한 번에 끝내고
 * 결과만 받는다.
 */
public interface WorkBookQuestionCount {

    Long getWorkBookId();

    long getQuestionCount();
}
