package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.QuestionInfo;
import com.buildup.nextQuestion.dto.solving.FindQuestionsByNormalExamResponse;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.QuestionInfoRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.utility.JwtUtility;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buildup.nextQuestion.dto.wrongNote.*;


import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WrongNoteService {

    private final EncryptionService encryptionService;
    private final JwtUtility jwtUtility;
    private final QuestionRepository questionRepository;
    private final LocalMemberRepository localMemberRepository;

    @Transactional
    public List<FindQuestionsByWrongNoteResponse> findQuestionsByWrongNote(String token, FindQuestionsByWrongNoteRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        List<FindQuestionsByWrongNoteResponse> response = new ArrayList<>();

        List<Question> wrongQuestions = questionRepository.findByMemberIdAndDelFalseAndWrongTrueAndRecentSolveTimeBetween(
                member.getId(), request.getStartDate(), request.getEndDate());

        if (wrongQuestions.isEmpty()) {
            throw new IllegalArgumentException("해당 문제를 찾을 수 없습니다.");
        }


        // 응답 생성
        for (Question question : wrongQuestions ) {
            QuestionInfo questionInfo = question.getQuestionInfo();

            FindQuestionsByWrongNoteResponse selectedQuestion = new FindQuestionsByWrongNoteResponse();
            selectedQuestion.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(question.getId()));
            selectedQuestion.setName(questionInfo.getName());
            selectedQuestion.setType(questionInfo.getType());
            selectedQuestion.setAnswer(questionInfo.getAnswer());
            selectedQuestion.setOpt(questionInfo.getOption());
            selectedQuestion.setRecentSolveTime(question.getRecentSolveTime());

            response.add(selectedQuestion);
        }

        return response;
    }
}
