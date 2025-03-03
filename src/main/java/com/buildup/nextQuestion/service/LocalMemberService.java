package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.dto.RegistDTORequest;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LocalMemberService {

    private final LocalMemberRepository localMemberRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LocalMember register(RegistDTORequest registDTORequest) {

        String userId = registDTORequest.getUserId();
        String password = registDTORequest.getPassword();
        String email = registDTORequest.getEmail();
        String nickname = registDTORequest.getNickname();
        // 중복 체크
        if (localMemberRepository.existsByUserId(userId) || localMemberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 아이디 또는 이메일입니다.");
        }

        // Member 객체 생성
        Member member = new Member(nickname, LoginType.LOCAL);
        memberRepository.save(member);

        // 비밀번호 해싱 후 LocalMember 생성
        String hashedPassword = passwordEncoder.encode(password);
        LocalMember localMember = new LocalMember(userId, hashedPassword, email, member);
        return localMemberRepository.save(localMember);
    }
}
