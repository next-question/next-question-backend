package com.buildup.nextQuestion.service.question;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 문서 분할 · 문제 수 배분 단위 테스트.
 *
 * <p>지켜야 할 성질은 두 가지다 — 어떤 조각도 입력 상한을 넘지 않을 것,
 * 그리고 유형별 문제 수 합계가 요청한 수와 정확히 같을 것.
 */
class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    @DisplayName("상한보다 짧은 문서는 자르지 않는다 — 요청 한 번으로 끝난다")
    void shortDocumentStaysWhole() {
        List<DocumentChunker.Chunk> chunks =
                chunker.chunk("짧은 문서입니다.", new QuestionTypeCounts(2, 2, 1), 1000);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).counts()).isEqualTo(new QuestionTypeCounts(2, 2, 1));
    }

    @Test
    @DisplayName("긴 문서는 나뉘고, 어떤 조각도 상한을 넘지 않는다")
    void noChunkExceedsLimit() {
        String document = paragraphs(60, 300); // 문단 60개 × 약 300자
        int maxChars = 1000;

        List<DocumentChunker.Chunk> chunks =
                chunker.chunk(document, new QuestionTypeCounts(10, 10, 10), maxChars);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.text().length()).isLessThanOrEqualTo(maxChars));
    }

    @Test
    @DisplayName("조각으로 나뉘어도 유형별 문제 수 합계는 그대로다")
    void totalsArePreserved() {
        String document = paragraphs(40, 500);
        QuestionTypeCounts requested = new QuestionTypeCounts(7, 11, 5);

        List<DocumentChunker.Chunk> chunks = chunker.chunk(document, requested, 900);

        assertThat(chunks.stream().mapToInt(c -> c.counts().ox()).sum()).isEqualTo(7);
        assertThat(chunks.stream().mapToInt(c -> c.counts().multiple()).sum()).isEqualTo(11);
        assertThat(chunks.stream().mapToInt(c -> c.counts().blank()).sum()).isEqualTo(5);
    }

    @Test
    @DisplayName("문제가 한 개도 배정되지 않은 조각은 요청 대상에서 빠진다")
    void chunksWithoutQuestionsAreDropped() {
        String document = paragraphs(50, 400);

        // 조각 수보다 적은 문제 수 — 일부 조각은 0개가 된다
        List<DocumentChunker.Chunk> chunks =
                chunker.chunk(document, new QuestionTypeCounts(0, 3, 0), 800);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.counts().isEmpty()).isFalse());
        assertThat(chunks.stream().mapToInt(c -> c.counts().total()).sum()).isEqualTo(3);
    }

    @Test
    @DisplayName("한 문단이 상한보다 길면 그 문단만 강제로 끊는다")
    void oversizedParagraphIsHardSplit() {
        String single = "가".repeat(2500);

        List<DocumentChunker.Chunk> chunks =
                chunker.chunk(single, new QuestionTypeCounts(3, 0, 0), 1000);

        assertThat(chunks.stream().map(DocumentChunker.Chunk::text).collect(Collectors.joining()))
                .hasSize(2500);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.text().length()).isLessThanOrEqualTo(1000));
    }

    @Test
    @DisplayName("빈 문서는 조각이 없다 — 호출부가 거절할 수 있게")
    void emptyDocumentYieldsNothing() {
        assertThat(chunker.chunk("   ", new QuestionTypeCounts(5, 0, 0), 1000)).isEmpty();
        assertThat(chunker.chunk(null, new QuestionTypeCounts(5, 0, 0), 1000)).isEmpty();
    }

    private static String paragraphs(int count, int lengthEach) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append("\n\n");
            sb.append("문단").append(i).append(" ").append("내용".repeat(lengthEach / 2));
        }
        return sb.toString();
    }
}
