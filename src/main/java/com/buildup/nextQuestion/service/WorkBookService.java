package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import com.buildup.nextQuestion.dto.workBook.CreateWorkBookRequest;
import com.buildup.nextQuestion.dto.workBook.GetWorkBookInfoResponse;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.MemberRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkBookService {

    private final JwtUtility jwtUtility;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final LocalMemberRepository localMemberRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public WorkBookInfo createWorkBook(String token, CreateWorkBookRequest request) {

        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).get().getMember();

        WorkBookInfo workBookInfo = new WorkBookInfo();
        workBookInfo.setMember(member);
        workBookInfo.setName(request.getWorkBookName());
        workBookInfo.setRecentSolveDate(null);

        return workBookInfoRepository.save(workBookInfo);
    }

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
}
