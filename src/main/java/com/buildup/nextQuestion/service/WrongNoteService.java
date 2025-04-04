package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.repository.SocialMemberRepository;
import com.buildup.nextQuestion.utility.JwtUtility;



import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buildup.nextQuestion.dto.wrongNote.*;


import java.sql.Timestamp;
import java.util.*;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WrongNoteService {

    private final EncryptionService encryptionService;
    private final JwtUtility jwtUtility;
    private final QuestionRepository questionRepository;
    private final LocalMemberRepository localMemberRepository;
    private final SocialMemberRepository socialMemberRepository;

    @Transactional
    public List<FindQuestionsByWrongNoteResponse> findQuestionsByWrongNote(String token, FindQuestionsByWrongNoteRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .map(LocalMember::getMember)
                .orElseGet(() -> socialMemberRepository.findBySnsId(userId)
                        .map(SocialMember::getMember)
                        .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                );

        Date startDate = java.sql.Timestamp.valueOf(request.getStartDate().atStartOfDay());
        Date endDate = java.sql.Timestamp.valueOf(request.getEndDate().atTime(23, 59, 59));

        //오답 문제 리스트
        List<Question> wrongQuestions = questionRepository.findByMemberIdAndDelFalseAndWrongTrueAndRecentSolveTimeBetween(
                member.getId(), startDate, endDate);

        if (wrongQuestions.isEmpty()) {
            throw new IllegalArgumentException("해당 문제를 찾을 수 없습니다.");
        }

        // 응답 생성
        List<FindQuestionsByWrongNoteResponse> response = new ArrayList<>();

        for (Question question : wrongQuestions) {
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
