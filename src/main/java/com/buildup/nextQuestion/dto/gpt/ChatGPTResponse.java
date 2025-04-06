package com.buildup.nextQuestion.dto.gpt;

import lombok.Data;

import java.util.List;

@Data
public class ChatGPTResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private FunctionCall function_call;
    }

    @Data
    public static class FunctionCall {
        private String name;
        private String arguments;
    }
}