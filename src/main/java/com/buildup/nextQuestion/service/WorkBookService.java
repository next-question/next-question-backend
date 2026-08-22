package com.buildup.nextQuestion.service;


import com.buildup.nextQuestion.exception.DuplicateResourceException;
import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.dto.workBook.*;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.repository.WorkBookRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.repository.projection.WorkBookQuestionCount;
import com.buildup.nextQuestion.support.MemberFinder;
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
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkBookService {

    private final JwtUtility jwtUtility;
    private final WorkBookRepository workBookRepository;
    private final EncryptionService encryptionService;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final QuestionRepository questionRepository;
    private final MemberFinder memberFinder;

    @Transactional
    public CreateWorkBookResponse createWorkBook(String token, CreateWorkBookRequest request) throws Exception {

        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

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

    /**
     * 문제집 목록. 문제집마다 삭제되지 않은 문제 수를 함께 내려준다.
     *
     * <p>문제 수는 문제집별 집계 쿼리 한 번으로 구한다. 예전에는 문제집마다 구성 목록을 조회하고
     * 그 안에서 문제를 하나씩 조회해 세었는데, 문제집 N개에 문제가 각 M개면 쿼리가
     * 1 + N + (N × M)개까지 늘어났다.
     */
    @Transactional(readOnly = true)
    public List<GetWorkBookResponse> getWorkBook(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        List<WorkBook> workBooks = workBookRepository.findAllByMemberId(member.getId());
        if (workBooks.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Long> questionCounts = countActiveQuestions(member.getId(), workBooks);

        List<GetWorkBookResponse> getWorkBookResponses = new ArrayList<>();
        for (WorkBook workBook : workBooks) {
            GetWorkBookResponse getWorkBookResponse = new GetWorkBookResponse();
            getWorkBookResponse.setEncryptedWorkBookId(encryptionService.encryptPrimaryKey(workBook.getId()));
            getWorkBookResponse.setName(workBook.getName());
            getWorkBookResponse.setRecentSolvedDate(workBook.getRecentSolveDate());
            getWorkBookResponse.setTotalQuestion(questionCounts.getOrDefault(workBook.getId(), 0L).intValue());
            getWorkBookResponses.add(getWorkBookResponse);
        }

        return getWorkBookResponses;
    }

    /** 문제집 id → 삭제되지 않은 문제 수. 문제가 없는 문제집은 결과에 없으므로 호출부가 0으로 채운다. */
    private Map<Long, Long> countActiveQuestions(Long memberId, List<WorkBook> workBooks) {
        List<Long> workBookIds = workBooks.stream().map(WorkBook::getId).toList();

        return workBookInfoRepository.countActiveQuestionsByWorkBookIds(memberId, workBookIds)
                .stream()
                .collect(Collectors.toMap(
                        WorkBookQuestionCount::getWorkBookId,
                        WorkBookQuestionCount::getQuestionCount));
    }

    public List<GetQuestionsByWorkBookResponse> searchQuestionsByWorkBook(String token, GetQuestionsByWorkBookRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

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
                response.setEncryptedQuestionInfoId(encryptionService.encryptPrimaryKey(questionInfo.getId()));
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
        Member member = memberFinder.findMember(userId);

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
        Member member = memberFinder.findMember(userId);

        String requestedWorkBookName = request.getName();

        Long workbookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());

        WorkBook workBook = workBookRepository.findById(workbookId).orElseThrow(
                () -> new EntityNotFoundException("문제집을 찾을 수 없습니다.")
        );

        Long requestedMemberId = workBook.getMember().getId();

        List<WorkBook> infos = workBookRepository.findByNameAndMemberId(requestedWorkBookName, requestedMemberId)
                .stream()
                .filter(wb -> !wb.getId().equals(workbookId))  // 자기 자신 제외
                .toList();

        if (!infos.isEmpty()) {
            throw new DuplicateResourceException("이미 존재하는 문제집입니다.");
        }

        if (!requestedMemberId.equals(member.getId())) {
            throw new SecurityException("문제집 업데이트에 오류가 발생했습니다.");
        }

        workBook.setName(requestedWorkBookName);
    }


}
