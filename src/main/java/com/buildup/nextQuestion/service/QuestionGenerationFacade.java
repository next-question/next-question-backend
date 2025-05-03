package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.dto.question.UploadFileByMemberResponse;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.utility.JwtUtility;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class QuestionGenerationFacade {
    private final FileService fileService;
    private final GPTService gptService;
    private final QuestionService questionService;
    private final JwtUtility jwtUtility;
    private final LocalMemberRepository localMemberRepository;

    public JsonNode generateQuestionByGuest(MultipartFile file) throws IOException {
        String content = fileService.extractTextFromPDF(file);
        return gptService.requestFunctionCalling(content, 10,10,10);
    }

    public List<UploadFileByMemberResponse> generateQuestionByMember(MultipartFile file, String numOfQuestions) throws Exception {

        String content = fileService.extractTextFromPDF(file);

        JsonNode response = gptService.requestFunctionCalling(content, 10, 10, 10);
        return questionService.saveAll(response);
    }
}

