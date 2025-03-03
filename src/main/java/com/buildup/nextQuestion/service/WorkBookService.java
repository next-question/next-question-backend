package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import com.buildup.nextQuestion.dto.workBook.CreateWorkBookRequest;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.MemberRepository;
import com.buildup.nextQuestion.repository.WorkBookInfoRepository;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkBookService {

    private final JwtUtility jwtUtility;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final LocalMemberRepository localMemberRepository;

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
}
