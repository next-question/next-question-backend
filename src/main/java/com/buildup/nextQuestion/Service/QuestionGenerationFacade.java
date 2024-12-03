package com.buildup.nextQuestion.Service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@AllArgsConstructor
public class QuestionGenerationFacade {
    private final FileService fileService;
    private final GPTService gptService;
    private final QuestionService questionService;

    public JsonNode createQuestionByGuest(MultipartFile file, int numOfQuestions) throws IOException {
        String content = fileService.extractTextFromPDF(file);
        String response = gptService.requestGPT(content, numOfQuestions);
        return gptService.stringToJson(response);
    }

    public JsonNode createQuestionByMember(MultipartFile file, int numOfQuestions)throws IOException {
        String content = fileService.extractTextFromPDF(file);
        String response = gptService.requestGPT(content, numOfQuestions);
        JsonNode jsonNode = gptService.stringToJson(response);
        questionService.saveAll(jsonNode);
        return jsonNode;
    }

}
