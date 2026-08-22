package com.buildup.nextQuestion.service.question;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 요청한 문제 수를 선택된 유형들에 나눈다.
 *
 * <p>똑같은 로직이 회원용·비회원용 두 곳에 복사돼 있던 것을 여기로 모았다.
 * 나누어떨어지지 않는 나머지는 선택된 유형 중 하나에 무작위로 얹는다.
 *
 * <p>난수를 생성자로 받는 이유는 테스트 때문이다. 예전에는 메서드 안에서 {@code new Random()}
 * 을 만들어 결과가 매번 달라졌고, 그래서 이 로직에는 테스트를 붙일 수 없었다.
 */
@Component
public class QuestionTypeDistributor {

    private static final String OX = "ox";
    private static final String MULTIPLE = "multiple";
    private static final String BLANK = "blank";

    private final Random random;

    public QuestionTypeDistributor() {
        this(new Random());
    }

    public QuestionTypeDistributor(Random random) {
        this.random = random;
    }

    /**
     * @param total    만들 문제 총 개수
     * @param ox       OX 선택 여부
     * @param multiple 객관식 선택 여부
     * @param blank    빈칸 선택 여부
     * @throws IllegalArgumentException 유형을 하나도 고르지 않았거나 총 개수가 0 이하일 때
     */
    public QuestionTypeCounts distribute(int total, boolean ox, boolean multiple, boolean blank) {
        List<String> selected = new ArrayList<>();
        if (ox) selected.add(OX);
        if (multiple) selected.add(MULTIPLE);
        if (blank) selected.add(BLANK);

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 문제 유형을 선택해야 합니다.");
        }
        if (total <= 0) {
            throw new IllegalArgumentException("문제 수는 1개 이상이어야 합니다.");
        }

        int base = total / selected.size();
        int remainder = total % selected.size();

        int oxCount = ox ? base : 0;
        int multipleCount = multiple ? base : 0;
        int blankCount = blank ? base : 0;

        for (int i = 0; i < remainder; i++) {
            switch (selected.get(random.nextInt(selected.size()))) {
                case OX -> oxCount++;
                case MULTIPLE -> multipleCount++;
                default -> blankCount++;
            }
        }

        return new QuestionTypeCounts(oxCount, multipleCount, blankCount);
    }
}
