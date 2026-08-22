package com.buildup.nextQuestion.dto.question;

import com.buildup.nextQuestion.service.question.GenerationJob;

import java.util.List;

/**
 * 비동기 문제 생성 작업의 현재 상태.
 *
 * <p>진행 중이면 questions·error 가 모두 비어 있고, 끝나면 둘 중 하나가 채워진다.
 */
public record QuestionGenerationJobResponse(
        String jobId,
        GenerationJob.Status status,
        List<UploadFileByMemberResponse> questions,
        String error
) {
    public static QuestionGenerationJobResponse from(GenerationJob job) {
        return new QuestionGenerationJobResponse(
                job.getId(), job.getStatus(), job.getQuestions(), job.getError());
    }
}
