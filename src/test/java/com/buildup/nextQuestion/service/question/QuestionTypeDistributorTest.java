package com.buildup.nextQuestion.service.question;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 유형 배분 로직 단위 테스트.
 *
 * <p>예전에는 이 로직이 메서드 안에서 {@code new Random()} 을 만들어 결과가 매번 달라졌고,
 * 그래서 테스트를 붙일 수 없었다. 난수를 생성자로 받도록 바꾸면서 고정 시드로 검증한다.
 */
class QuestionTypeDistributorTest {

    private final QuestionTypeDistributor distributor = new QuestionTypeDistributor(new Random(42));

    @Test
    @DisplayName("나누어떨어지면 유형마다 같은 수를 준다")
    void splitsEvenly() {
        QuestionTypeCounts counts = distributor.distribute(9, true, true, true);

        assertThat(counts.ox()).isEqualTo(3);
        assertThat(counts.multiple()).isEqualTo(3);
        assertThat(counts.blank()).isEqualTo(3);
        assertThat(counts.total()).isEqualTo(9);
    }

    @Test
    @DisplayName("나머지가 있어도 총합은 요청한 수와 정확히 같다")
    void remainderNeverChangesTotal() {
        for (int total = 1; total <= 50; total++) {
            assertThat(distributor.distribute(total, true, true, true).total())
                    .as("총 %d개 요청", total)
                    .isEqualTo(total);
        }
    }

    @Test
    @DisplayName("선택하지 않은 유형에는 한 개도 배정하지 않는다")
    void unselectedTypesGetNothing() {
        QuestionTypeCounts counts = distributor.distribute(7, true, false, true);

        assertThat(counts.multiple()).isZero();
        assertThat(counts.ox() + counts.blank()).isEqualTo(7);
    }

    @Test
    @DisplayName("한 유형만 고르면 전부 그 유형이다")
    void singleTypeTakesAll() {
        QuestionTypeCounts counts = distributor.distribute(11, false, true, false);

        assertThat(counts.multiple()).isEqualTo(11);
        assertThat(counts.ox()).isZero();
        assertThat(counts.blank()).isZero();
    }

    @Test
    @DisplayName("유형을 하나도 고르지 않으면 거절한다")
    void rejectsNoType() {
        assertThatThrownBy(() -> distributor.distribute(5, false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 하나의 문제 유형");
    }

    @Test
    @DisplayName("문제 수가 0 이하면 거절한다")
    void rejectsNonPositiveTotal() {
        assertThatThrownBy(() -> distributor.distribute(0, true, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 시드면 같은 결과 — 재현 가능하다")
    void deterministicWithSameSeed() {
        QuestionTypeCounts first = new QuestionTypeDistributor(new Random(7)).distribute(10, true, true, true);
        QuestionTypeCounts second = new QuestionTypeDistributor(new Random(7)).distribute(10, true, true, true);

        assertThat(first).isEqualTo(second);
    }
}
