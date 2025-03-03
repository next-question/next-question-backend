package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.repository.QuestionInfoByMemberRepository;
import com.buildup.nextQuestion.domain.QuestionInfoByMember;
import com.buildup.nextQuestion.dto.question.QuestionUpdateRequest;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.domain.Question;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service

public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;
    private QuestionInfoByMemberRepository questionInfoByMemberRepository;

    //생성된 문제 리스트 저장
    public void saveAll(JsonNode jsonNode) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode questionsNode = jsonNode.get("questions");
        if (questionsNode != null && questionsNode.isArray()) {

            List<Question> questions = objectMapper.readValue(
                    questionsNode.toString(),
                    new TypeReference<List<Question>>() {
                    }
            );
            questionRepository.saveAll(questions);
        }
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
}

