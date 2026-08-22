package com.buildup.nextQuestion.service.question;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 긴 문서를 모델이 감당할 수 있는 크기로 자르고, 만들 문제 수를 조각별로 나눈다.
 *
 * <p>예전에는 PDF 에서 뽑은 텍스트 전체를 프롬프트 뒤에 그대로 붙였다. 문서가 길면 모델의
 * 입력 한도를 넘겨 요청이 통째로 실패했고, 넘지 않더라도 입력 토큰이 페이지 수에 그대로
 * 비례해 늘었다. 길이 검사도 없었다.
 *
 * <p>자르는 기준은 문단이다. 문단 경계에서 끊어야 문장이 잘리지 않아 요약·문제 생성 품질이
 * 덜 상한다. 한 문단이 한도보다 길면 그때만 강제로 끊는다.
 */
@Component
public class DocumentChunker {

    /** 한 조각의 최대 글자 수. 조각 하나가 곧 요청 한 번의 입력이 된다. */
    public record Chunk(String text, QuestionTypeCounts counts) {
    }

    /**
     * 문서를 자르고 유형별 문제 수를 조각에 배분한다.
     *
     * <p>배분은 조각 길이에 비례한다. 반올림에서 남는 몫은 소수부가 큰 조각부터 하나씩 얹어
     * 유형별 합계가 요청한 수와 정확히 일치하도록 맞춘다.
     *
     * @return 만들 문제가 하나 이상 배정된 조각들. 배정이 0인 조각은 요청하지 않으므로 제외된다.
     */
    public List<Chunk> chunk(String text, QuestionTypeCounts counts, int maxChars) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("조각 최대 글자 수는 1 이상이어야 합니다.");
        }

        List<String> pieces = split(text == null ? "" : text, maxChars);
        if (pieces.isEmpty()) {
            return List.of();
        }
        if (pieces.size() == 1) {
            return List.of(new Chunk(pieces.get(0), counts));
        }

        List<Integer> weights = pieces.stream().map(String::length).toList();
        List<Integer> ox = allocate(counts.ox(), weights);
        List<Integer> multiple = allocate(counts.multiple(), weights);
        List<Integer> blank = allocate(counts.blank(), weights);

        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            QuestionTypeCounts chunkCounts =
                    new QuestionTypeCounts(ox.get(i), multiple.get(i), blank.get(i));
            if (!chunkCounts.isEmpty()) {
                chunks.add(new Chunk(pieces.get(i), chunkCounts));
            }
        }
        return chunks;
    }

    /** 문단 경계에서 자르되, 한 문단이 한도보다 길면 그 문단만 강제로 끊는다. */
    private List<String> split(String text, int maxChars) {
        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        if (trimmed.length() <= maxChars) {
            return List.of(trimmed);
        }

        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : trimmed.split("\\R{2,}")) {
            for (String part : hardSplit(paragraph, maxChars)) {
                if (current.length() > 0 && current.length() + part.length() + 2 > maxChars) {
                    pieces.add(current.toString().strip());
                    current.setLength(0);
                }
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(part);
            }
        }
        if (current.length() > 0) {
            pieces.add(current.toString().strip());
        }
        return pieces;
    }

    /** 문단 하나가 한도를 넘을 때만 쓰는 마지막 수단. */
    private List<String> hardSplit(String paragraph, int maxChars) {
        if (paragraph.length() <= maxChars) {
            return List.of(paragraph);
        }
        List<String> parts = new ArrayList<>();
        for (int start = 0; start < paragraph.length(); start += maxChars) {
            parts.add(paragraph.substring(start, Math.min(paragraph.length(), start + maxChars)));
        }
        return parts;
    }

    /**
     * 총량을 가중치에 비례해 정수로 나눈다(최대 잔여법).
     * 합계는 항상 {@code total} 과 정확히 같다.
     */
    private List<Integer> allocate(int total, List<Integer> weights) {
        List<Integer> result = new ArrayList<>(java.util.Collections.nCopies(weights.size(), 0));
        if (total <= 0) {
            return result;
        }

        long weightSum = weights.stream().mapToLong(Integer::longValue).sum();
        if (weightSum <= 0) {
            result.set(0, total);
            return result;
        }

        List<double[]> remainders = new ArrayList<>();
        int assigned = 0;
        for (int i = 0; i < weights.size(); i++) {
            double exact = total * (weights.get(i) / (double) weightSum);
            int floor = (int) Math.floor(exact);
            result.set(i, floor);
            assigned += floor;
            remainders.add(new double[]{i, exact - floor});
        }

        remainders.sort(Comparator.comparingDouble((double[] r) -> r[1]).reversed());
        for (int i = 0; assigned < total; i++, assigned++) {
            int index = (int) remainders.get(i % remainders.size())[0];
            result.set(index, result.get(index) + 1);
        }
        return result;
    }
}
