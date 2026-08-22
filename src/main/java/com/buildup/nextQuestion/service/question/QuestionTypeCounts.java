package com.buildup.nextQuestion.service.question;

/**
 * 유형별 문제 수 — OX / 객관식 / 빈칸.
 *
 * <p>예전에는 {@code int[]{ox, multiple, blank}} 로 넘겨서 호출부마다 인덱스 순서를 기억해야 했다.
 */
public record QuestionTypeCounts(int ox, int multiple, int blank) {

    public static final QuestionTypeCounts NONE = new QuestionTypeCounts(0, 0, 0);

    public int total() {
        return ox + multiple + blank;
    }

    public boolean isEmpty() {
        return total() == 0;
    }
}
