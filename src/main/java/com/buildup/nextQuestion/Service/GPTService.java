package com.buildup.nextQuestion.Service;

import com.buildup.nextQuestion.dto.ChatGPTRequest;
import com.buildup.nextQuestion.dto.ChatGPTResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GPTService {


    @Value("${openai.prompt}")
    private String prompt;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiURL;

    @Autowired
    private RestTemplate template;


    public String requestGPT(String sourceText, int numOfQuestions){

        prompt += "\n" + sourceText;
        ChatGPTRequest request = new ChatGPTRequest(model, prompt);
        ChatGPTResponse chatGPTResponse =  template.postForObject(apiURL, request, ChatGPTResponse.class);

        return chatGPTResponse.getChoices().get(0).getMessage().getContent();

    }

    public JsonNode stringToJson(String response) throws JsonProcessingException {
        String codeBlockStart = "```json";
        String codeBlockEnd = "```";

        int startIndex = response.indexOf(codeBlockStart);
        int endIndex = response.indexOf(codeBlockEnd, startIndex + codeBlockStart.length());

        if (startIndex == -1 || endIndex == -1) {
            throw new IllegalStateException("Invalid input: JSON code block not found.");
        }

        // JSON 문자열 추출
        String jsonString = response.substring(startIndex + codeBlockStart.length(), endIndex).trim();
        System.out.println(jsonString);
        // JSON 문자열을 JsonNode로 변환
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readTree(jsonString);

    }





}
