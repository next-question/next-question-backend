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

    public JsonNode generateQuestionGuest(MultipartFile sourceFile, int numOfQuestions) throws IOException {
        String sourceText = fileService.extractTextFromPDF(sourceFile);
        String respone = gptService.requestGPT(sourceText, numOfQuestions);
        System.out.println(respone);
        return gptService.stringToJson(respone);
    }

//    public void generateQuestionMember(MultipartFile sorceFile, int numOfQuestions)throws IOException {
//        String sourceText = fileService.extractTextFromPDF(sorceFile);
//        String respone = gptService.requestGPT(sourceText, numOfQuestions);
//        questionService.saveAll(respone);
//    }

}
