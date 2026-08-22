package com.buildup.nextQuestion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * GPT 호출 재시도 정책 테스트.
 *
 * <p>예전에는 {@code new RestTemplate()} 을 그대로 써서 타임아웃도 재시도도 없었다.
 * OpenAI 가 잠깐 흔들리면 그 요청은 그냥 실패했고, 응답이 없으면 요청 스레드가 끝없이 묶였다.
 *
 * <p>여기서 확인하는 것은 두 가지다 — 일시적인 실패는 다시 보내는가, 그리고
 * <b>다시 보내도 소용없는 실패는 즉시 포기하는가</b>. 후자가 더 중요하다. 잘못된 요청을
 * 반복해서 보내면 실패한 요청에 돈만 더 쓴다.
 */
class GPTServiceRetryTest {

    private static final String API_URL = "http://openai.test/v1/chat/completions";

    private static final String SUCCESS_BODY = """
            {"choices":[{"message":{"function_call":{"arguments":"{\\"questions\\":[]}"}}}]}
            """;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private GPTService gptService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        gptService = new GPTService(restTemplate);
        ReflectionTestUtils.setField(gptService, "model", "test-model");
        ReflectionTestUtils.setField(gptService, "apiURL", API_URL);
        ReflectionTestUtils.setField(gptService, "promptTemplate", "총 %d문항(객관식 %d, 빈칸 %d, OX %d): ");
        ReflectionTestUtils.setField(gptService, "maxAttempts", 3);
        ReflectionTestUtils.setField(gptService, "retryBackoffMs", 1L);
    }

    @Test
    @DisplayName("서버 오류(5xx)는 다시 보낸다 — 두 번 실패 후 성공")
    void retriesServerError() {
        server.expect(requestTo(API_URL)).andRespond(withServerError());
        server.expect(requestTo(API_URL)).andRespond(withServerError());
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        assertThat(gptService.requestFunctionCalling("문서", 1, 1, 1)).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("요청 과다(429)도 다시 보낸다")
    void retriesTooManyRequests() {
        server.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        assertThat(gptService.requestFunctionCalling("문서", 1, 1, 1)).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("잘못된 요청(400)은 다시 보내지 않는다 — 실패한 요청에 돈을 더 쓰지 않는다")
    void doesNotRetryBadRequest() {
        server.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> gptService.requestFunctionCalling("문서", 1, 1, 1))
                .isInstanceOf(RestClientResponseException.class);

        // 기대한 요청은 딱 한 번뿐 — 두 번째가 왔다면 verify 가 실패한다
        server.verify();
    }

    @Test
    @DisplayName("재시도 횟수를 다 쓰면 마지막 예외를 그대로 올린다")
    void givesUpAfterMaxAttempts() {
        server.expect(requestTo(API_URL)).andRespond(withServerError());
        server.expect(requestTo(API_URL)).andRespond(withServerError());
        server.expect(requestTo(API_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> gptService.requestFunctionCalling("문서", 1, 1, 1))
                .isInstanceOf(RestClientResponseException.class);

        server.verify();
    }

    @Test
    @DisplayName("재시도 대상 판별 — 5xx·429·연결오류만 true")
    void retryableClassification() {
        assertThat(gptService.isRetryable(
                new org.springframework.web.client.ResourceAccessException("timeout"))).isTrue();
        assertThat(gptService.isRetryable(
                new RestClientResponseException("unavailable", 503, "unavailable", null, null, null))).isTrue();
        assertThat(gptService.isRetryable(
                new RestClientResponseException("too many", 429, "too many", null, null, null))).isTrue();
        assertThat(gptService.isRetryable(
                new RestClientResponseException("bad request", 400, "bad request", null, null, null))).isFalse();
        assertThat(gptService.isRetryable(
                new RestClientResponseException("unauthorized", 401, "unauthorized", null, null, null))).isFalse();
        assertThat(gptService.isRetryable(new IllegalStateException("응답이 비어있음"))).isFalse();
    }
}
