package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.repository.*;
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
    private final LocalMemberRepository localMemberRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final HistoryInfoRepository historyInfoRepository;
    private final HistoryRepository historyRepository;

    @Transactional
    public FindQuestionsByWrongNoteResponse findQuestionsByWrongNote(String token, FindQuestionsByWrongNoteRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .map(LocalMember::getMember)
                .orElseGet(() -> socialMemberRepository.findBySnsId(userId)
                        .map(SocialMember::getMember)
                        .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                );

        //periodType 판별
        LocalDate startLocalDate;
        LocalDate endLocalDate;

        String period = request.getPeriodType(); // "week", "custom", "month"

        if ("custom".equalsIgnoreCase(period)) {
            startLocalDate = request.getStartDate();
            endLocalDate = request.getEndDate();
        } else if ("month".equalsIgnoreCase(period)) {
            startLocalDate = LocalDate.now().minusDays(30);
            endLocalDate = LocalDate.now();
        } else { // 기본값: week
            startLocalDate = LocalDate.now().minusDays(7);
            endLocalDate = LocalDate.now();
        }

        Timestamp startDate = Timestamp.valueOf(startLocalDate.atStartOfDay());
        Timestamp endDate = Timestamp.valueOf(endLocalDate.atTime(23, 59, 59));

        //해당 기간 문제 리스트
        List<History> memberHistory = historyRepository.findAllByMemberIdAndSolvedDateBetween(member.getId(), startDate, endDate);

        if (memberHistory.isEmpty()) {
            throw new EntityNotFoundException("해당 기간 문제를 찾을 수 없습니다.");
        }

        //해당 기간 틀린 문제 찾기
        List<HistoryInfo> wrongHistoryInfos = historyInfoRepository
                .findByWrongTrue(memberHistory);
        if (wrongHistoryInfos.isEmpty()) {
            throw new EntityNotFoundException("해당 기간 오답 문제를 찾을 수 없습니다.");
        }

        //중복 제거(최신 문제만 남김)
        Map<Long, HistoryInfo> latestByQuestionInfoId = new HashMap<>();
        for (HistoryInfo h : wrongHistoryInfos) {
            Long questionInfoId = h.getQuestion().getQuestionInfo().getId();
            if (!latestByQuestionInfoId.containsKey(questionInfoId) ||
                    h.getHistory().getSolvedDate().after(latestByQuestionInfoId.get(questionInfoId).getHistory().getSolvedDate())) {
                latestByQuestionInfoId.put(questionInfoId, h);
            }
        }

        // 응답 생성
        List<FindQuestionsByWrongNoteDTO> result = new ArrayList<>();

        for (HistoryInfo historyInfo : latestByQuestionInfoId.values()) {
            Question question = historyInfo.getQuestion();
            QuestionInfo questionInfo = question.getQuestionInfo();

            FindQuestionsByWrongNoteDTO selectedQuestion = new FindQuestionsByWrongNoteDTO();
            selectedQuestion.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(question.getId()));
            selectedQuestion.setName(questionInfo.getName());
            selectedQuestion.setType(questionInfo.getType());
            selectedQuestion.setAnswer(questionInfo.getAnswer());
            selectedQuestion.setOpt(questionInfo.getOption());
            selectedQuestion.setRecentSolveTime(question.getRecentSolveTime());
            selectedQuestion.setSolvedDate(historyInfo.getHistory().getSolvedDate());

            result.add(selectedQuestion);
        }

        FindQuestionsByWrongNoteResponse response = new FindQuestionsByWrongNoteResponse();
        response.setQuestions(result);
        response.setStartDate(startLocalDate);
        response.setEndDate(endLocalDate);

        return response;
    }
}
