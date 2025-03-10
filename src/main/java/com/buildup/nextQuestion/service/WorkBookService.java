package com.buildup.nextQuestion.service;


import com.buildup.nextQuestion.exception.DuplicateResourceException;
import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.dto.workBook.*;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.repository.WorkBookRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.utility.JwtUtility;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkBookService {

    private final JwtUtility jwtUtility;
    private final WorkBookRepository workBookRepository;
    private final LocalMemberRepository localMemberRepository;
    private final EncryptionService encryptionService;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public CreateWorkBookResponse createWorkBook(String token, CreateWorkBookRequest request) throws Exception {

        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();
        String requestedWorkBookName = request.getWorkBookName();

        List<WorkBook> infos = workBookRepository.findByNameAndMemberId(requestedWorkBookName, member.getId());
        if (!infos.isEmpty()) {
            throw new DuplicateResourceException("이미 존재하는 문제집입니다.");
        }

        WorkBook workBook = new WorkBook();
        workBook.setMember(member);
        workBook.setName(requestedWorkBookName);
        workBook.setRecentSolveDate(new Timestamp(System.currentTimeMillis()));
        Long workBookInfoId = workBookRepository.save(workBook).getId();
        CreateWorkBookResponse createWorkBookResponse = new CreateWorkBookResponse();
        createWorkBookResponse.setEncryptedWorkBookId(encryptionService.encryptPrimaryKey(workBookInfoId));

        return createWorkBookResponse;

    }

    @Transactional
    public List<GetWorkBookResponse> getWorkBook(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        List<WorkBook> workBooks = workBookRepository.findAllByMemberId(member.getId());
        if (workBooks.isEmpty()) {
            throw new EntityNotFoundException("해당 사용자의 문제집이 존재하지 않습니다.");
        }


        List<GetWorkBookResponse> getWorkBookResponses = new ArrayList<>();
        int totalQuestion = 0;
        for (WorkBook workBook : workBooks) {
            for (WorkBookInfo workBookInfo : workBookInfoRepository.findAllByWorkBookId(workBook.getId())) {
                Long questionInfoId = workBookInfo.getQuestionInfo().getId();
                Question question = questionRepository.findByMemberIdAndQuestionInfoId(member.getId(), questionInfoId).get();
                if (!question.getDel())
                    totalQuestion++;
            }

            GetWorkBookResponse getWorkBookResponse = new GetWorkBookResponse();
            getWorkBookResponse.setEncryptedWorkBookId(encryptionService.encryptPrimaryKey(workBook.getId()));
            getWorkBookResponse.setName(workBook.getName());
            getWorkBookResponse.setRecentSolvedDate(workBook.getRecentSolveDate());
            getWorkBookResponse.setTotalQuestion(totalQuestion);
            getWorkBookResponses.add(getWorkBookResponse);
        }

        return getWorkBookResponses;
    }

    public List<GetQuestionsByWorkBookResponse> searchQuestionsByWorkBook(String token, GetQuestionsByWorkBookRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."))
                .getMember();

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());
        //해당 문제집 찾기
        WorkBook workBook = workBookRepository.findById(workBookId).orElseThrow(
                () -> new EntityNotFoundException("문제집이 존재하지 않습니다."));

        if (!workBookRepository.existsByIdAndMemberId(workBookId, member.getId())){
            throw new AccessDeniedException("사용자의 문제집이 아닙니다.");
        }
        List<WorkBookInfo> workBookInfos = workBookInfoRepository.findAllByWorkBookId(workBookId);
        List<GetQuestionsByWorkBookResponse> responses = new ArrayList<>();

        for (WorkBookInfo workBookInfo : workBookInfos) {
            QuestionInfo questionInfo = workBookInfo.getQuestionInfo();

            GetQuestionsByWorkBookResponse response = new GetQuestionsByWorkBookResponse();
            Question question = questionRepository.findByMemberIdAndQuestionInfoId(member.getId(), questionInfo.getId()).get();
            if (!question.getDel()) {
                response.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(question.getId()));
                response.setName(questionInfo.getName());
                response.setType(questionInfo.getType());
                response.setAnswer(questionInfo.getAnswer());
                response.setOpt(questionInfo.getOption());
                response.setCreateTime(questionInfo.getCreateTime());
                response.setRecentSolveTime(question.getRecentSolveTime());

                responses.add(response);
            }
        }
        return responses;

    }

    @SneakyThrows
    @Transactional
    public void deleteWorkBook(String token, List<String> encryptedWorkBookIds) throws Exception {

        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."))
                .getMember();

        List<Long> decryptedIds = encryptedWorkBookIds.stream()
                .map(encryptedId -> {
                    try {
                        return encryptionService.decryptPrimaryKey(encryptedId);
                    } catch (Exception e) {
                        throw new RuntimeException("키 복호화 중 오류 발생: " + encryptedId, e);
                    }
                })
                .toList();


        List<WorkBook> userWorkBooks = workBookRepository.findAllByMemberId(member.getId());

        if (userWorkBooks.isEmpty()) {
            throw new EntityNotFoundException("사용자의 문제집이 존재하지 않습니다.");
        }

        List<WorkBook> workBooksToDelete = userWorkBooks.stream()
                .filter(workBook -> decryptedIds.contains(workBook.getId()))
                .collect(Collectors.toList());

        if (workBooksToDelete.size() != decryptedIds.size()) {
            throw new SecurityException("문제집 삭제에 오류가 발생했습니다.");
        }

        for (WorkBook workBook : workBooksToDelete) {
            List<WorkBookInfo> workBookInfos = workBookInfoRepository.findAllByWorkBookId(workBook.getId());
            for (WorkBookInfo workBookInfo : workBookInfos) {
                questionRepository.deleteByMemberIdAndQuestionInfoId(member.getId(), workBookInfo.getQuestionInfo().getId());
                workBookInfoRepository.delete(workBookInfo);
            }
            workBookRepository.delete(workBook);

        }

    }

    @Transactional
    public void updateWorkBook(String token, UpdateWorkBookRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."))
                .getMember();

        String requestedWorkBookName = request.getName();

        List<WorkBook> infos = workBookRepository.findByNameAndMemberId(requestedWorkBookName, member.getId());
        if (!infos.isEmpty()) {
            throw new DuplicateResourceException("이미 존재하는 문제집입니다.");
        }

        Long workbookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());
        WorkBook workBook = workBookRepository.findById(workbookId).orElseThrow(
                () -> new EntityNotFoundException("문제집을 찾을 수 없습니다."));
        Long requestedMemberId = workBook.getMember().getId();

        // 삭제하려는 문제집이 해당 토큰의 멤버 문제집인지 검증
        if (!requestedMemberId.equals(member.getId())) {
            throw new SecurityException("문제집 업데이트에 오류가 발생했습니다.");
        }

        workBook.setName(requestedWorkBookName);
    }


}
