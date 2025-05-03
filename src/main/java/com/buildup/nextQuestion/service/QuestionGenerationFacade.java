package com.buildup.nextQuestion.service;


import com.buildup.nextQuestion.dto.question.UploadFileByGuestRequest;
import com.buildup.nextQuestion.dto.question.UploadFileByMemberRequest;
import com.buildup.nextQuestion.dto.question.UploadFileByMemberResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@AllArgsConstructor
public class QuestionGenerationFacade {
    private final FileService fileService;
    private final GPTService gptService;
    private final QuestionService questionService;

    public JsonNode generateQuestionByGuest(UploadFileByGuestRequest request) throws IOException {
        String content = fileService.extractTextFromPDF(request.getFile());

        // 선택 유형 비율에 맞게 5문제 분배
        int[] counts = distributeQuestionCountForGuest(request.getOx(), request.getMultiple(), request.getBlank());
        int ox = counts[0];
        int multiple = counts[1];
        int blank = counts[2];

        JsonNode response = gptService.requestFunctionCalling(content, multiple, blank, ox);

        // level 필드 제거
        if (response.has("questions")) {
            for (JsonNode question : response.get("questions")) {
                ((ObjectNode) question).remove("level");
            }
        }

        return response;
    }


    public List<UploadFileByMemberResponse> generateQuestionByMember(UploadFileByMemberRequest request) throws Exception {

        String content = fileService.extractTextFromPDF(request.getFile());

        // 유형별 문제 개수 분배
        int[] counts = distributeQuestionCount(request.getQuestionCount(), request.getOx(), request.getMultiple(), request.getBlank());
        int ox = counts[0];
        int multiple = counts[1];
        int blank = counts[2];

        JsonNode response = gptService.requestFunctionCalling(content, multiple, blank, ox);

        return questionService.saveAll(response);
    }


    private int[] distributeQuestionCount(int total ,boolean ox, boolean multiple, boolean blank) {

        // 어떤 유형이 선택됐는지 확인
        List<String> selectedTypes = new ArrayList<>();
        if (Boolean.TRUE.equals(ox)) selectedTypes.add("ox");
        if (Boolean.TRUE.equals(multiple)) selectedTypes.add("multiple");
        if (Boolean.TRUE.equals(blank)) selectedTypes.add("blank");

        int typeCount = selectedTypes.size();
        if (typeCount == 0) {
            throw new IllegalArgumentException("최소 하나의 문제 유형을 선택해야 합니다.");
        }

        int baseCount = total / typeCount;
        int remainder = total % typeCount;

        Map<String, Integer> counts = new HashMap<>();
        for (String type : selectedTypes) {
            counts.put(type, baseCount);
        }

        // 부족분을 랜덤 유형에 추가
        Random random = new Random();
        for (int i = 0; i < remainder; i++) {
            String randomType = selectedTypes.get(random.nextInt(typeCount));
            counts.put(randomType, counts.get(randomType) + 1);
        }

        // 결과 반환 (ox, multiple, blank 순서)
        int oxCount = counts.getOrDefault("ox", 0);
        int multipleCount = counts.getOrDefault("multiple", 0);
        int blankCount = counts.getOrDefault("blank", 0);

        return new int[]{oxCount, multipleCount, blankCount};
    }

    private int[] distributeQuestionCountForGuest(boolean ox, boolean multiple, boolean blank) {
        int total = 5;

        List<String> selectedTypes = new ArrayList<>();
        if (Boolean.TRUE.equals(ox)) selectedTypes.add("ox");
        if (Boolean.TRUE.equals(multiple)) selectedTypes.add("multiple");
        if (Boolean.TRUE.equals(blank)) selectedTypes.add("blank");

        int typeCount = selectedTypes.size();
        if (typeCount == 0) {
            throw new IllegalArgumentException("최소 하나의 문제 유형을 선택해야 합니다.");
        }

        int baseCount = total / typeCount;
        int remainder = total % typeCount;

        Map<String, Integer> counts = new HashMap<>();
        for (String type : selectedTypes) {
            counts.put(type, baseCount);
        }

        Random random = new Random();
        for (int i = 0; i < remainder; i++) {
            String randomType = selectedTypes.get(random.nextInt(typeCount));
            counts.put(randomType, counts.get(randomType) + 1);
        }

        int oxCount = counts.getOrDefault("ox", 0);
        int multipleCount = counts.getOrDefault("multiple", 0);
        int blankCount = counts.getOrDefault("blank", 0);

        return new int[]{oxCount, multipleCount, blankCount};
    }

}

