package com.buildup.nextQuestion.service.question;

import com.buildup.nextQuestion.dto.question.UploadFileByMemberResponse;

import java.time.Instant;
import java.util.List;

/**
 * 문제 생성 작업 한 건의 상태.
 *
 * <p>생성은 GPT 응답을 기다리는 동안 길게는 수십 초가 걸린다. 예전에는 그 시간 내내
 * HTTP 요청 하나가 통째로 묶여 있었고, 중간에 게이트웨이 타임아웃이 나면 이미 쓴 GPT 비용은
 * 날아가고 결과도 남지 않았다. 작업을 이 객체로 떼어내 백그라운드에서 돌린다.
 */
public class GenerationJob {

    public enum Status {
        /** 실행 중. 클라이언트는 잠시 뒤 다시 물어보면 된다. */
        RUNNING,
        /** 성공. {@link #getQuestions()} 에 결과가 있다. */
        SUCCEEDED,
        /** 실패. {@link #getError()} 에 사유가 있다. */
        FAILED
    }

    private final String id;
    private final Instant createdAt;

    private volatile Status status = Status.RUNNING;
    private volatile List<UploadFileByMemberResponse> questions;
    private volatile String error;
    private volatile Instant finishedAt;

    public GenerationJob(String id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public void succeed(List<UploadFileByMemberResponse> questions, Instant at) {
        this.questions = questions;
        this.finishedAt = at;
        this.status = Status.SUCCEEDED;
    }

    public void fail(String error, Instant at) {
        this.error = error;
        this.finishedAt = at;
        this.status = Status.FAILED;
    }

    public boolean isFinished() {
        return status != Status.RUNNING;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public List<UploadFileByMemberResponse> getQuestions() {
        return questions;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
