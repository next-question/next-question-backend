package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.dto.gpt.ChatGPTRequest;
import com.buildup.nextQuestion.dto.gpt.ChatGPTResponse;
import com.buildup.nextQuestion.dto.gpt.FunctionSpec;
import com.buildup.nextQuestion.dto.gpt.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GPTService {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiURL;

    @Value("${gpt.prompt}")
    private String promptTemplate;

    private final RestTemplate template;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GPTService(RestTemplate template) {
        this.template = template;
    }

    public JsonNode requestFunctionCalling(String documentText, int multiCount, int fillCount, int oxCount) {
        int totalCount = multiCount + fillCount + oxCount;

        // 프롬프트 생성
        String prompt = String.format(promptTemplate, totalCount, multiCount, fillCount, oxCount);

        List<Message> messages = List.of(
                new Message("user", prompt + documentText)
        );

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
                                                "type", Map.of("type", "string", "enum", List.of("MULTIPLE_CHOICE", "FILL_IN_THE_BLANK", "OX")),
                                                "level", Map.of("type", "string", "enum", List.of("low", "medium", "high")),
                                                "option", Map.of("type", "string"),
                                                "answer", Map.of("type", "string")
                                        )
                                )
                        )
                ),
                "required", List.of("questions")
        );

        generateQuestionsFn.setParameters(parameters);

        ChatGPTRequest request = new ChatGPTRequest(
                model,
                messages,
                List.of(generateQuestionsFn),
                functionCall
        );

        try {
            ChatGPTResponse response = template.postForObject(apiURL, request, ChatGPTResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("GPT 응답이 비어있습니다.");
            }

            String arguments = Optional.ofNullable(response.getChoices().get(0))
                    .map(ChatGPTResponse.Choice::getMessage)
                    .map(ChatGPTResponse.Message::getFunction_call)
                    .map(ChatGPTResponse.FunctionCall::getArguments)
                    .orElseThrow(() -> new RuntimeException("GPT 응답에서 arguments를 찾을 수 없습니다."));

            return objectMapper.readTree(arguments);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("GPT 응답 JSON 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("GPT 요청 실패: " + e.getMessage(), e);
        }
    }
}
