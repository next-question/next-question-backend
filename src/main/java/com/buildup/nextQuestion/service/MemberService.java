package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.dto.member.FindMembersResponse;
import com.buildup.nextQuestion.dto.member.LoginRequest;
import com.buildup.nextQuestion.dto.member.LoginResponse;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.MemberRepository;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly=true)
@RequiredArgsConstructor
public class MemberService {

    private final LocalMemberRepository localMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtility jwtUtility;
    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;

    public LoginResponse login(LoginRequest loginDTOrequest) {
        String userId = loginDTOrequest.getUserId();
        String password = loginDTOrequest.getPassword();
        // 1. userId로 회원 조회
        LocalMember localMember = localMemberRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        Member member = localMember.getMember();
        // 2. 비밀번호 검증
        if (passwordEncoder.matches(password, localMember.getPassword())) {
            LoginResponse loginDTOresponse = new LoginResponse(jwtUtility.generateToken(userId, member.getRole()), member.getNickname(), member.getRole());
            // 3. JWT 토큰 생성 후 반환
            return loginDTOresponse;
        } else {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public List<FindMembersResponse> findMembers() throws Exception {
        List<Member> members = memberRepository.findAll();
        List<FindMembersResponse> response = new ArrayList<>();
        for (Member member : members) {
            FindMembersResponse memberInfo = new FindMembersResponse();
            memberInfo.setEncryptedMemberId(encryptionService.encryptPrimaryKey(member.getId()));
            memberInfo.setNickname(member.getNickname());
            memberInfo.setLoginType(member.getLogintype());
            response.add(memberInfo);
        }
        return response;
    }
}
