package com.buildup.nextQuestion.Service;

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

    public String generateQuestionGuest(MultipartFile sourceFile, int numOfQuestions)throws IOException {
        String sourceText = fileService.extractTextFromPDF(sourceFile);
        String respone = gptService.requestGPT(sourceText, numOfQuestions);
        return respone;
    }

    public void generateQuestionMember(MultipartFile sorceFile, int numOfQuestions)throws IOException {
        String sourceText = fileService.extractTextFromPDF(sorceFile);
        String respone = gptService.requestGPT(sourceText, numOfQuestions);
        questionService.saveParsedQuestions(respone);
    }

}
