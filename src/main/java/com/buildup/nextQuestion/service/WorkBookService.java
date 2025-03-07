package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import com.buildup.nextQuestion.dto.workBook.CreateWorkBookRequest;
import com.buildup.nextQuestion.dto.workBook.CreateWorkBookResponse;
import com.buildup.nextQuestion.dto.workBook.GetWorkBookInfoResponse;
import com.buildup.nextQuestion.dto.workBook.UpdateWorkBookInfoRequest;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.QuestionInfoByMemberRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.repository.WorkBookRepository;
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
    private final WorkBookInfoRepository workBookInfoRepository;
    private final LocalMemberRepository localMemberRepository;
    private final EncryptionService encryptionService;
    private final WorkBookRepository workBookRepository;
    private final QuestionInfoByMemberRepository questionInfoByMemberRepository;

    @Transactional
    public CreateWorkBookResponse createWorkBook(String token, CreateWorkBookRequest request) throws Exception {

        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).get().getMember();
        String requestedWorkBookName = request.getWorkBookName();

        List<WorkBookInfo> infos = workBookInfoRepository.findByName(requestedWorkBookName);
        if (!infos.isEmpty()) {
            throw new IllegalArgumentException("이미 존재하는 문제집입니다.");
        }

        WorkBookInfo workBookInfo = new WorkBookInfo();
        workBookInfo.setMember(member);
        workBookInfo.setName(requestedWorkBookName);
        workBookInfo.setRecentSolveDate(null);
        Long workBookInfoId = workBookInfoRepository.save(workBookInfo).getId();
        CreateWorkBookResponse createWorkBookResponse = new CreateWorkBookResponse();
        createWorkBookResponse.setEncryptedWorkBookInfoId(encryptionService.encryptPrimaryKey(workBookInfoId));

        return createWorkBookResponse;

    }

    @Transactional
    public List<GetWorkBookInfoResponse> getWorkBookInfo(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).get().getMember();

        List<WorkBookInfo> workBookInfos = workBookInfoRepository.findAllByMemberId(member.getId());

        List<GetWorkBookInfoResponse> getWorkBookInfoResponses = new ArrayList<>();
        for (WorkBookInfo workBookInfo : workBookInfos) {
            GetWorkBookInfoResponse getWorkBookInfoResponse = new GetWorkBookInfoResponse();

            getWorkBookInfoResponse.setEncryptedWorkBookInfoId(encryptionService.encryptPrimaryKey(workBookInfo.getId()));
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


        List<WorkBookInfo> userWorkBooks = workBookInfoRepository.findAllByMemberId(member.getId());

        if (userWorkBooks.isEmpty()) {
            throw new IllegalArgumentException("사용자의 문제집이 존재하지 않습니다.");
        }

        List<WorkBookInfo> workBooksToDelete = userWorkBooks.stream()
                .filter(workBook -> decryptedIds.contains(workBook.getId()))
                .collect(Collectors.toList());

        if (workBooksToDelete.size() != decryptedIds.size()) {
            throw new SecurityException("문제집 삭제에 오류가 발생했습니다.");
        }

        for (WorkBookInfo workBookInfo : workBooksToDelete) {
            List<WorkBook> workBooks = workBookRepository.findAllByWorkBookInfoId(workBookInfo.getId());
            for (WorkBook workBook : workBooks) {
                questionInfoByMemberRepository.deleteByMemberIdAndQuestionId(member.getId(), workBook.getId());
                workBookRepository.delete(workBook);
            }
            workBookInfoRepository.delete(workBookInfo);

        }

    }

    @Transactional
    public void updateWorkBookInfo(String token, UpdateWorkBookInfoRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).get().getMember();

        String requestedWorkBookName = request.getName();

        List<WorkBookInfo> infos = workBookInfoRepository.findByName(requestedWorkBookName);
        if (!infos.isEmpty()) {
            throw new IllegalArgumentException("이미 존재하는 문제집입니다.");
        }

        Long workbookInfoId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookInfoId());
        WorkBookInfo workBookInfo = workBookInfoRepository.findById(workbookInfoId).orElseThrow(
                () -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Long requestedMemberId = workBookInfo.getMember().getId();

        // 삭제하려는 문제집이 해당 토큰의 멤버 문제집인지 검증
        if (!requestedMemberId.equals(member.getId())) {
            throw new SecurityException("문제집 업데이트에 오류가 발생했습니다.");
        }

        workBookInfo.setName(requestedWorkBookName);
    }


}
