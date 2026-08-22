package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.dto.gpt.ChatGPTRequest;
import com.buildup.nextQuestion.dto.gpt.ChatGPTResponse;
import com.buildup.nextQuestion.dto.gpt.FunctionSpec;
import com.buildup.nextQuestion.dto.gpt.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GPTService {

    private static final Logger log = LoggerFactory.getLogger(GPTService.class);

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiURL;

    @Value("${gpt.prompt}")
    private String promptTemplate;

    /**
     * 실패 시 총 시도 횟수(첫 시도 포함). 1이면 재시도하지 않는다.
     *
     * <p>재시도 대상은 <b>일시적인 실패만</b>이다 — 타임아웃·연결 오류·5xx·429. 잘못된 요청(4xx)은
     * 다시 보내도 같은 결과라 즉시 포기한다. 그러지 않으면 실패한 요청에 돈만 더 쓴다.
     */
    @Value("${openai.max-attempts:3}")
    private int maxAttempts;

    /** 재시도 간격. 시도할 때마다 두 배로 늘린다. */
    @Value("${openai.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    private final RestTemplate template;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GPTService(RestTemplate template) {
        this.template = template;
    }

    public JsonNode requestFunctionCalling(String documentText, int multiCount, int fillCount, int oxCount) {
        int totalCount = multiCount + fillCount + oxCount;
        ChatGPTRequest request = buildRequest(documentText, totalCount, multiCount, fillCount, oxCount);

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            try {
                return callOnce(request);
            } catch (RuntimeException e) {
                if (!isRetryable(e) || attempt == Math.max(1, maxAttempts)) {
                    throw e;
                }
                lastFailure = e;
                long waitMs = retryBackoffMs * (1L << (attempt - 1));
                log.warn("GPT 요청 실패({}/{}), {}ms 후 재시도: {}",
                        attempt, maxAttempts, waitMs, e.getMessage());
                sleep(waitMs);
            }
        }
        throw lastFailure; // 도달하지 않는다 — 마지막 시도는 위에서 그대로 던진다
    }

    private JsonNode callOnce(ChatGPTRequest request) {
        ChatGPTResponse response = template.postForObject(apiURL, request, ChatGPTResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalStateException("GPT 응답이 비어있습니다.");
        }

        String arguments = Optional.ofNullable(response.getChoices().get(0))
                .map(ChatGPTResponse.Choice::getMessage)
                .map(ChatGPTResponse.Message::getFunction_call)
                .map(ChatGPTResponse.FunctionCall::getArguments)
                .orElseThrow(() -> new IllegalStateException("GPT 응답에서 arguments를 찾을 수 없습니다."));

        try {
            return objectMapper.readTree(arguments);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("GPT 응답 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    /** 다시 보내면 달라질 수 있는 실패인가. */
    boolean isRetryable(RuntimeException e) {
        // 타임아웃·연결 실패 — 네트워크 문제라 다시 보낼 가치가 있다
        if (e instanceof ResourceAccessException) {
            return true;
        }
        // 429(요청 과다)와 5xx(서버 문제)만 재시도. 나머지 4xx 는 요청 자체가 잘못된 것이다
        if (e instanceof RestClientResponseException http) {
            int status = http.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }

    private ChatGPTRequest buildRequest(String documentText, int totalCount,
                                        int multiCount, int fillCount, int oxCount) {
        String prompt = String.format(promptTemplate, totalCount, multiCount, fillCount, oxCount);

        List<Message> messages = List.of(new Message("user", prompt + documentText));
        Map<String, String> functionCall = Map.of("name", "generate_questions");

        FunctionSpec generateQuestionsFn = new FunctionSpec();
        generateQuestionsFn.setName("generate_questions");
        generateQuestionsFn.setDescription("문서를 기반으로 문제 " + totalCount + "개를 생성");

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", Map.of(
                        "questions", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "name", Map.of("type", "string"),
                                                "type", Map.of("type", "string", "enum",
                                                        List.of("MULTIPLE_CHOICE", "FILL_IN_THE_BLANK", "OX")),
                                                "level", Map.of("type", "string", "enum",
                                                        List.of("low", "medium", "high")),
                                                "option", Map.of("type", "string"),
                                                "answer", Map.of("type", "string")
                                        )
                                )
                        )
                ),
                "required", List.of("questions")
        );
        generateQuestionsFn.setParameters(parameters);

        return new ChatGPTRequest(model, messages, List.of(generateQuestionsFn), functionCall);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GPT 재시도 대기 중 중단됨", e);
        }
    }
}
