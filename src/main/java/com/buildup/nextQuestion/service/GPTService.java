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

    private final RestTemplate template;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GPTService(RestTemplate template) {
        this.template = template;
    }

    public JsonNode requestFunctionCalling(String documentText, int multiCount, int fillCount, int oxCount) {
        int totalCount = multiCount + fillCount + oxCount;

        List<Message> messages = List.of(
                new Message("user", buildPrompt(documentText, totalCount, multiCount, fillCount, oxCount))
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
                                                "type", Map.of("type", "string", "enum", List.of("MULTIPLE_CHOICE", "FILL_BLANK", "OX")),
                                                "level", Map.of("type", "string", "enum", List.of("low", "medium", "high")),
                                                "option", Map.of("type", "string"),
                                                "answer", Map.of("type", "string")
                                        ),
                                        "required", List.of("name", "type", "level", "answer")
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


    private String buildPrompt(String content, int totalCount, int multiCount, int fillCount, int oxCount) {
        return """
        너는 전문 문제 출제자야.

        주어진 문서를 바탕으로 **총 %d개의 문제**를 생성하되, 아래의 기준을 반드시 지켜야 해

        [문제 생성 기준]
        - 문제 유형별 개수:
          - MULTIPLE_CHOICE: %d개
          - FILL_BLANK: %d개
          - OX: %d개

        [출제 조건]
        - 반드시 주어진 문서의 내용을 기반으로만 문제를 출제할 것 (외부 지식 금지)
        - 문제를 만들때 해당 학문의 핵심 개념, 정리, 원리를 바탕으로 문제를 만들것.
        - 문서의 핵심 주제(제일 많이 차지하는 주제)가 아닌 주제로는 문제를 생성하지 말 것.
        - 유형 별로 문제를 확실히 구분할 것.
    
        [유형별 출제 조건]
        - OX: 단순 암기 여부를 묻기보다는, 개념의 참뜻을 이해하고 있는지를 확인할 수 있어야 하며, 흔히 혼동되거나 오해하기 쉬운 지점을 정교하게 짚어내는 방식으로 출제되어야 한다.
        - FILL_BLANK :  빈칸으로 설정하면 안되는 곳 알려줄께. 단순 수식어, 형용사, 학습 효과 낮고 정답 유도 쉬운곳. 자주 쓰이는 일반 명사나 핵심 단어가 아닌 것.
        - MULTIPLE_CHOICE : 객관식 문제는 학습자의 개념 이해나 응용 능력을 효과적으로 측정할 수 있어야 한다. 정답을 단순히 암기해서 맞히기보다는 추론을 통해 도출하도록 유도해야 한다. 혼동되는 개념을 정교한 오답으로 제시해 선택 간 구분을 요구한다.

        [문제 형식 예시]
        {
          "questions": [
            {
              "name": "텍스트 전처리의 목적은 무엇인가?",
              "type": "MULTIPLE_CHOICE",
              "level": "low",
              "option": "1. 데이터 저장|||2. 텍스트 분석을 위한 사전 작업|||3. 이미지 처리|||4. 웹 수집",
              "answer": "2"
            },
            {
              "name": "컴퓨터 네트워킹에서, {BLANK}은 데이터를 송수신하는 기능만을 담당하는 OSI 모델의 첫 번째 계층이다.",
              "type": "FILL_BLANK",
              "level": "low",
              "answer": "물리 계층"
            },
            {
              "name": "서브워드 토큰화는 OOV 문제를 줄이기 위해 고안된 방법이다.",
              "type": "OX",
              "level": "medium",
              "answer": "O"
            }
          ]
        }

        [문제 출제를 위한 문서]
        """.formatted(totalCount, multiCount, fillCount, oxCount) + content;
    }


}

