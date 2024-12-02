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

    public JsonNode generateQuestionByGuest(MultipartFile file, int numOfQuestions) throws IOException {
        String content = fileService.extractTextFromPDF(file);
        String response = gptService.requestGPT(content, numOfQuestions);
        System.out.println(response);
        return gptService.stringToJson(response);
    }

//    public void generateQuestionMember(MultipartFile file, int numOfQuestions)throws IOException {
//        String content = fileService.extractTextFromPDF(file);
//        String response = gptService.requestGPT(response, numOfQuestions);
//        questionService.saveAll(response);
//    }

}
