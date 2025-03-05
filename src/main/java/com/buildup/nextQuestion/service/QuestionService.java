package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.dto.question.SaveQuestionRequest;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.dto.question.QuestionUpdateRequest;
import com.buildup.nextQuestion.utility.JwtUtility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {


    private final QuestionRepository questionRepository;
    private final QuestionInfoByMemberRepository questionInfoByMemberRepository;
    private final EncryptionService encryptionService;
    private final JwtUtility jwtUtility;
    private final LocalMemberRepository localMemberRepository;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final WorkBookRepository workBookRepository;

    //생성된 문제 리스트 저장
    public List<String> saveAll(JsonNode jsonNode) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode questionsNode = jsonNode.get("questions");
        List<String> encryptedQeustionIds = new ArrayList<>();
        if (questionsNode != null && questionsNode.isArray()) {

            List<Question> questions = objectMapper.readValue(
                    questionsNode.toString(),
                    new TypeReference<List<Question>>() {
                    }
            );
            List<Question> createdQuestions = questionRepository.saveAll(questions);

            for (Question createdQuestion : createdQuestions) {
                String encryptedQuestionId = encryptionService.encryptPrimaryKey(createdQuestion.getId());
                encryptedQeustionIds.add(encryptedQuestionId);

            }
        }

        return encryptedQeustionIds;
    }

    //문제 정보 갱신(update)
    public void updateQuestion (List<QuestionUpdateRequest> updatedQuestions) {
        for(QuestionUpdateRequest request : updatedQuestions) {
            Long questionId = request.getQuestionId();
            QuestionInfoByMember existingQuestion = questionInfoByMemberRepository.findById(questionId).get(); //id로 Question객체 가져옴
            existingQuestion.setWrong(request.getWrong()); //객체 정답여부 update
            existingQuestion.setRecentSolveTime(request.getRecentSolveTime()); //객체 최근 시간 update

            questionInfoByMemberRepository.save(existingQuestion); //객체 저장
        }
    }

    public void saveQuestion (String token, SaveQuestionRequest saveQuestionRequest) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getMember();


        WorkBookInfo workBookInfo = workBookInfoRepository.findById(
                encryptionService.decryptPrimaryKey(
                        saveQuestionRequest.getEncryptedWorkBookInfoId()))
                .get();
        for (String encryptedQeustionId : saveQuestionRequest.getEncryptedQuestionIds()) {
            Question question = questionRepository.findById(
                    encryptionService.decryptPrimaryKey(encryptedQeustionId))
                    .get();

            WorkBook workBook = new WorkBook(question, workBookInfo);
            workBookRepository.save(workBook);
        }

    }
}

