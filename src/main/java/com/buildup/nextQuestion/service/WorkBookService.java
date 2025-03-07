package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.dto.workBook.CreateWorkBookRequest;
import com.buildup.nextQuestion.dto.workBook.CreateWorkBookResponse;
import com.buildup.nextQuestion.dto.workBook.GetWorkBookInfoResponse;
import com.buildup.nextQuestion.dto.workBook.UpdateWorkBookInfoRequest;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.repository.WorkBookRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Member member = localMemberRepository.findByUserId(userId).get().getMember();
        String requestedWorkBookName = request.getWorkBookName();

        List<WorkBook> infos = workBookRepository.findByName(requestedWorkBookName);
        if (!infos.isEmpty()) {
            throw new IllegalArgumentException("이미 존재하는 문제집입니다.");
        }

        WorkBook workBook = new WorkBook();
        workBook.setMember(member);
        workBook.setName(requestedWorkBookName);
        workBook.setRecentSolveDate(null);
        Long workBookInfoId = workBookRepository.save(workBook).getId();
        CreateWorkBookResponse createWorkBookResponse = new CreateWorkBookResponse();
        createWorkBookResponse.setEncryptedWorkBookId(encryptionService.encryptPrimaryKey(workBookInfoId));

        return createWorkBookResponse;

    }

    @Transactional
    public List<GetWorkBookInfoResponse> getWorkBookInfo(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).get().getMember();

        List<WorkBook> workBookInfos = workBookRepository.findAllByMemberId(member.getId());

        List<GetWorkBookInfoResponse> getWorkBookInfoResponses = new ArrayList<>();
        for (WorkBook workBookInfo : workBookInfos) {
            GetWorkBookInfoResponse getWorkBookInfoResponse = new GetWorkBookInfoResponse();

            getWorkBookInfoResponse.setEncryptedWorkBookId(encryptionService.encryptPrimaryKey(workBookInfo.getId()));
            getWorkBookInfoResponse.setName(workBookInfo.getName());

            getWorkBookInfoResponses.add(getWorkBookInfoResponse);
        }

        return getWorkBookInfoResponses;
    }

    @SneakyThrows
    @Transactional
    public void deleteWorkBookInfo(String token, List<String> encryptedWorkBookInfoIds) throws Exception {

        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."))
                .getMember();

        List<Long> decryptedIds = encryptedWorkBookInfoIds.stream()
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
            throw new IllegalArgumentException("사용자의 문제집이 존재하지 않습니다.");
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
    public void updateWorkBookInfo(String token, UpdateWorkBookInfoRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).get().getMember();

        String requestedWorkBookName = request.getName();

        List<WorkBook> infos = workBookRepository.findByName(requestedWorkBookName);
        if (!infos.isEmpty()) {
            throw new IllegalArgumentException("이미 존재하는 문제집입니다.");
        }

        Long workbookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());
        WorkBook workBook = workBookRepository.findById(workbookId).orElseThrow(
                () -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Long requestedMemberId = workBook.getMember().getId();

        // 삭제하려는 문제집이 해당 토큰의 멤버 문제집인지 검증
        if (!requestedMemberId.equals(member.getId())) {
            throw new SecurityException("문제집 업데이트에 오류가 발생했습니다.");
        }

        workBook.setName(requestedWorkBookName);
    }


}
