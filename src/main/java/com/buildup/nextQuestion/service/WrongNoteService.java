package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;



import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buildup.nextQuestion.dto.wrongNote.*;


import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WrongNoteService {

    private final MemberFinder memberFinder;
    private final EncryptionService encryptionService;
    private final JwtUtility jwtUtility;
    private final HistoryInfoRepository historyInfoRepository;
    private final HistoryRepository historyRepository;
    private final WorkBookRepository workBookRepository;
    private final WorkBookInfoRepository workBookInfoRepository;

    @Transactional
    public FindWrongNoteResponse findWrongNote(String token, FindWrongNoteRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

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

        //해당 기간 historyId 불러오기
        List<Long> historyIds = memberHistory.stream()
                .map(History::getId)  // 각 History 객체에서 historyId 추출
                .toList();

        List<HistoryInfo> wrongHistoryInfos = historyInfoRepository.findByWrongIsTrueAndHistoryIdIn(historyIds);
        if (wrongHistoryInfos.isEmpty()) {
            throw new EntityNotFoundException("해당 기간 오답 문제를 찾는 도중 오류가 발생했습니다.");
        }

        Map<String, GroupedWorkBookDTO> workBookMap = new HashMap<>(); //historyId, GroupedWorkBookDTO 묶기

        //문제집 찾기
        List<WorkBook> workBooks = workBookRepository.findAllByMemberId(member.getId());
        if (request.getWorkBookName() != null && !request.getWorkBookName().isEmpty()) {
            workBooks = workBooks.stream()
                    .filter(workBook -> workBook.getName().equalsIgnoreCase(request.getWorkBookName()))
                    .collect(Collectors.toList());
        }


        for (HistoryInfo historyInfo : wrongHistoryInfos) {
            String historyId = historyInfo.getHistory().getId().toString();
            Question question = historyInfo.getQuestion();
            QuestionInfo questionInfo = question.getQuestionInfo();
            Timestamp solvedDate = historyInfo.getHistory().getSolvedDate();

            WorkBookInfo workBookInfo = workBookInfoRepository.findByWorkBookInAndQuestionInfoId(workBooks, questionInfo.getId());
            WorkBook workBook = workBookInfo.getWorkBook();
            String workBookName = workBook.getName();
            String encryptedWorkBookId = encryptionService.encryptPrimaryKey(workBook.getId());

            GroupedWorkBookDTO groupedWorkBook = workBookMap.computeIfAbsent(historyId, k -> {
                GroupedWorkBookDTO newGroup = new GroupedWorkBookDTO();
                newGroup.setHistoryId(historyId);
                newGroup.setMainWorkBookName(workBookName); // 여기서 메인 워크북 이름 설정
                newGroup.setWorkBookCount(0);
                newGroup.setQuestionCount(0);
                newGroup.setSolvedDate(solvedDate);
                newGroup.setWorkBooks(new ArrayList<>());
                return newGroup;
            });

            // 이미 존재하는 workBook에 문제를 추가하거나 새로운 문제집을 추가
            WrongNoteWorkBookDTO workBookGroup = groupedWorkBook.getWorkBooks().stream()
                    .filter(wb -> wb.getEncryptedWorkBookId().equals(encryptedWorkBookId))
                    .findFirst()
                    .orElseGet(() -> {
                        WrongNoteWorkBookDTO newWorkBook = new WrongNoteWorkBookDTO();
                        newWorkBook.setEncryptedWorkBookId(encryptedWorkBookId);
                        newWorkBook.setWorkBookName(workBookName);
                        groupedWorkBook.setWorkBookCount(groupedWorkBook.getWorkBookCount() + 1);
                        groupedWorkBook.getWorkBooks().add(newWorkBook);
                        return newWorkBook;
                    });

            groupedWorkBook.setQuestionCount(groupedWorkBook.getQuestionCount() + 1);
        }


        List<GroupedWorkBookDTO> groupedWorkBooks = new ArrayList<>(workBookMap.values());

// 응답 객체에 groupedWorkBooks 추가
        FindWrongNoteResponse response = new FindWrongNoteResponse();
        response.setGroupedWorkBooks(groupedWorkBooks);
        response.setStartDate(startLocalDate);
        response.setEndDate(endLocalDate);
        return response;
    }

    @Transactional
    public FindQuestionsByWrongNoteResponse findQuestionsByWrongNote(String token, FindQuestionsByWrongNoteRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        FindQuestionsByWrongNoteResponse response = new FindQuestionsByWrongNoteResponse();
        List<Long> historyIds = request.getHistoryIds();  // 받아온 historyId 리스트

        // 틀린 문제만 필터링하여 찾기
        List<HistoryInfo> wrongHistoryInfos = historyInfoRepository.findByWrongIsTrueAndHistoryIdIn(historyIds);
        if (wrongHistoryInfos.isEmpty()) {
            throw new EntityNotFoundException("해당 기간 오답 문제를 찾는 도중 오류가 발생했습니다.");
        }

        List<WrongNoteQuestionDTO> result = new ArrayList<>();
        for (HistoryInfo historyInfo : wrongHistoryInfos) {
            Question question = historyInfo.getQuestion();
            QuestionInfo questionInfo = question.getQuestionInfo();

            // 문제 DTO 생성
            WrongNoteQuestionDTO questionDTO = new WrongNoteQuestionDTO();
            questionDTO.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(questionInfo.getId()));
            questionDTO.setName(questionInfo.getName());
            questionDTO.setType(questionInfo.getType());
            questionDTO.setAnswer(questionInfo.getAnswer());
            questionDTO.setOpt(questionInfo.getOption());
            questionDTO.setRecentSolvedDate(question.getRecentSolveTime());

            result.add(questionDTO);
        }

        response.setQuestions(result);

        return response;
    }

}