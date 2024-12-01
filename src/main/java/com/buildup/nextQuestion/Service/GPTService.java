package com.buildup.nextQuestion.Service;

import org.springframework.stereotype.Service;

@Service
public class GPTService {

    public String requestGPT(String sourceText, int numOfQuestions){
        String respone = "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"type\": \"MULTIPLE_CHOICE\",\n" +
                "      \"question\": \"What is the capital of France?\",\n" +
                "      \"options\": {\n" +
                "        \"1\": \"Berlin\",\n" +
                "        \"2\": \"Madrid\",\n" +
                "        \"3\": \"Paris\",\n" +
                "        \"4\": \"Rome\",\n" +
                "        \"5\": \"London\"\n" +
                "      },\n" +
                "      \"answer\": \"3\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 2,\n" +
                "      \"type\": \"SHORT_ANSWER\",\n" +
                "      \"question\": \"Which planet is known as the Red Planet?\",\n" +
                "      \"options\": {\n" +
                "        \"1\": \"Mars\",\n" +
                "        \"2\": \"Jupiter\",\n" +
                "        \"3\": \"Saturn\",\n" +
                "        \"4\": \"Earth\",\n" +
                "        \"5\": \"Venus\"\n" +
                "      },\n" +
                "      \"answer\": \"1\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        return respone;
    }

}
