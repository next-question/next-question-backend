package com.buildup.nextQuestion.service.question;

import com.buildup.nextQuestion.dto.question.UploadFileByMemberResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Callable;

/**
 * 문제 생성을 백그라운드로 돌리고 상태를 조회할 수 있게 한다.
 *
 * <p>제출은 즉시 반환하고 실제 생성은 별도 스레드에서 진행한다. 클라이언트는 받은 작업 id 로
 * 상태를 물어본다. 생성이 오래 걸려도 HTTP 요청이 그동안 묶여 있지 않고, 게이트웨이 타임아웃에
 * 결과가 통째로 날아가지도 않는다.
 *
 * <p><b>한계</b> — 작업 상태를 메모리에 들고 있어 서버를 재시작하면 진행 중이던 작업이 사라지고,
 * 인스턴스가 여러 대면 제출한 서버에서만 조회된다. 지금은 단일 인스턴스라 이 방식으로 충분하다.
 * 다중화하면 작업 테이블이나 Redis 로 옮겨야 한다.
 */
@Service
public class QuestionGenerationJobService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationJobService.class);

    private final Map<String, GenerationJob> jobs = new ConcurrentHashMap<>();
    private final Executor executor;

    /** 보관할 작업 수 상한. 넘으면 끝난 작업 중 오래된 것부터 버린다. */
    @Value("${question.generation.job-capacity:200}")
    private int capacity = 200;

    public QuestionGenerationJobService(@Qualifier("questionGenerationExecutor") Executor executor) {
        this.executor = executor;
    }

    /**
     * 생성 작업을 제출하고 즉시 작업 id 를 돌려준다.
     *
     * @param work 실제 생성 작업. 요청 스레드가 아니라 백그라운드 스레드에서 실행된다.
     * @throws RejectedExecutionException 대기열이 가득 차 지금은 받을 수 없을 때
     */
    public GenerationJob submit(Callable<List<UploadFileByMemberResponse>> work) {
        GenerationJob job = new GenerationJob(UUID.randomUUID().toString(), Instant.now());
        jobs.put(job.getId(), job);
        evictOldFinishedJobs();

        try {
            executor.execute(() -> run(job, work));
        } catch (RejectedExecutionException e) {
            jobs.remove(job.getId());
            throw e;
        }
        return job;
    }

    public Optional<GenerationJob> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    private void run(GenerationJob job, Callable<List<UploadFileByMemberResponse>> work) {
        try {
            job.succeed(work.call(), Instant.now());
        } catch (Exception e) {
            log.warn("문제 생성 작업 {} 실패: {}", job.getId(), e.toString());
            job.fail(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), Instant.now());
        }
    }

    /** 끝난 작업만 버린다. 진행 중인 작업은 상한을 넘더라도 남긴다. */
    private void evictOldFinishedJobs() {
        if (jobs.size() <= capacity) {
            return;
        }
        List<GenerationJob> finished = jobs.values().stream()
                .filter(GenerationJob::isFinished)
                .sorted(Comparator.comparing(GenerationJob::getCreatedAt))
                .toList();

        int toRemove = jobs.size() - capacity;
        for (int i = 0; i < Math.min(toRemove, finished.size()); i++) {
            jobs.remove(finished.get(i).getId());
        }
    }
}
