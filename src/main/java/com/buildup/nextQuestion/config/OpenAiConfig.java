package com.buildup.nextQuestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key}")
    private String openAiKey;

    /**
     * 연결 타임아웃. 이 시간 안에 OpenAI 와 TCP 연결이 되지 않으면 포기한다.
     */
    @Value("${openai.timeout.connect-ms:10000}")
    private long connectTimeoutMs;

    /**
     * 응답 타임아웃. 문제 생성은 원래 오래 걸리는 요청이라 넉넉히 잡되, <b>무한 대기는 막는다.</b>
     *
     * <p>예전에는 {@code new RestTemplate()} 을 그대로 썼다. 기본 설정에는 타임아웃이 없어서
     * OpenAI 가 응답하지 않으면 요청 스레드가 끝없이 묶여 있었다.
     */
    @Value("${openai.timeout.read-ms:120000}")
    private long readTimeoutMs;

    @Bean
    public RestTemplate template(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().add("Authorization", "Bearer " + openAiKey);
                    return execution.execute(request, body);
                })
                .build();
    }
}
