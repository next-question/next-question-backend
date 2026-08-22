package com.buildup.nextQuestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 문제 생성 전용 스레드 풀.
 *
 * <p>요청 처리 스레드(톰캣)와 분리한다. 생성은 GPT 응답을 기다리는 긴 작업이라, 같은 풀을 쓰면
 * 생성 몇 건이 몰렸을 때 일반 조회 요청까지 함께 막힌다.
 *
 * <p>대기열이 가득 차면 <b>거절한다</b>(호출자 스레드에서 대신 실행하지 않는다). 밀어내기로
 * 받아 두면 결국 사용자가 오래 기다리게 되므로, 지금은 받을 수 없다고 바로 알리는 편이 낫다.
 */
@Configuration
public class QuestionGenerationExecutorConfig {

    @Value("${question.generation.pool-size:2}")
    private int poolSize;

    @Value("${question.generation.queue-capacity:50}")
    private int queueCapacity;

    @Bean("questionGenerationExecutor")
    public Executor questionGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("question-gen-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
