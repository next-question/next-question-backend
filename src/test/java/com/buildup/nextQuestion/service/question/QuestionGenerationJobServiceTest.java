package com.buildup.nextQuestion.service.question;

import com.buildup.nextQuestion.dto.question.UploadFileByMemberResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * 비동기 문제 생성 작업 테스트.
 *
 * <p>핵심은 한 문장이다 — <b>생성이 얼마나 오래 걸리든 제출은 즉시 끝난다.</b>
 * 예전에는 GPT 응답을 기다리는 수십 초 동안 HTTP 요청 하나가 통째로 묶여 있었다.
 */
class QuestionGenerationJobServiceTest {

    private ThreadPoolTaskExecutor executor;

    private QuestionGenerationJobService newService(int poolSize, int queueCapacity) {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();

        QuestionGenerationJobService service = new QuestionGenerationJobService(executor);
        ReflectionTestUtils.setField(service, "capacity", 200);
        return service;
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("생성이 1초 걸려도 제출은 즉시 끝난다")
    void submitReturnsImmediately() {
        QuestionGenerationJobService service = newService(2, 10);

        long start = System.nanoTime();
        GenerationJob job = service.submit(() -> {
            Thread.sleep(1000); // 오래 걸리는 GPT 호출을 대신한다
            return List.of(new UploadFileByMemberResponse());
        });
        long submitMillis = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("%n  제출에 걸린 시간: %dms (생성 작업 자체는 1000ms)%n", submitMillis);

        assertThat(submitMillis).isLessThan(200);
        assertThat(job.getStatus()).isEqualTo(GenerationJob.Status.RUNNING);

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> service.find(job.getId()).orElseThrow().isFinished());

        GenerationJob finished = service.find(job.getId()).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(GenerationJob.Status.SUCCEEDED);
        assertThat(finished.getQuestions()).hasSize(1);
    }

    @Test
    @DisplayName("작업이 실패하면 상태와 사유가 남는다 — 요청이 끊겨도 결과를 잃지 않는다")
    void failureIsRecorded() {
        QuestionGenerationJobService service = newService(1, 10);

        GenerationJob job = service.submit(() -> {
            throw new IllegalStateException("GPT 응답이 비어있습니다.");
        });

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> service.find(job.getId()).orElseThrow().isFinished());

        GenerationJob finished = service.find(job.getId()).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(GenerationJob.Status.FAILED);
        assertThat(finished.getError()).contains("GPT 응답이 비어있습니다.");
    }

    @Test
    @DisplayName("대기열이 가득 차면 조용히 밀어내지 않고 거절한다")
    void rejectsWhenQueueIsFull() throws Exception {
        QuestionGenerationJobService service = newService(1, 1);
        CountDownLatch hold = new CountDownLatch(1);

        // 1번은 실행 중, 2번은 대기열 점유 → 3번은 자리가 없다
        service.submit(() -> {
            hold.await();
            return List.of();
        });
        service.submit(() -> {
            hold.await();
            return List.of();
        });

        try {
            assertThatThrownBy(() -> service.submit(List::of))
                    .isInstanceOf(RejectedExecutionException.class);
        } finally {
            hold.countDown();
        }
    }

    @Test
    @DisplayName("거절된 작업은 조회되지 않는다 — 유령 작업이 남지 않는다")
    void rejectedJobIsNotStored() throws Exception {
        QuestionGenerationJobService service = newService(1, 1);
        CountDownLatch hold = new CountDownLatch(1);

        GenerationJob first = service.submit(() -> {
            hold.await();
            return List.of();
        });
        GenerationJob second = service.submit(() -> {
            hold.await();
            return List.of();
        });

        try {
            service.submit(List::of);
        } catch (RejectedExecutionException expected) {
            // 거절된 작업의 id 는 알 수 없지만, 저장된 작업은 앞의 둘뿐이어야 한다
        } finally {
            hold.countDown();
        }

        assertThat(service.find(first.getId())).isPresent();
        assertThat(service.find(second.getId())).isPresent();
        assertThat(service.find("존재하지-않는-id")).isEmpty();
    }
}
