package com.buildup.nextQuestion.service.security;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.MemberRepository;
import com.buildup.nextQuestion.repository.SocialMemberRepository;
import com.buildup.nextQuestion.service.security.RefreshTokenService;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class SecurityService {

    private final MemberRepository memberRepository;
    private final LocalMemberRepository localMemberRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtility jwtUtility;

    // 리프레시 토큰 검증 후 액세스 토큰 재발급
    public String refreshAccessToken(String refreshToken) {
        // 1. 리프레시 토큰으로 사용자 찾기
        Member member = memberRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 리프레시 토큰입니다."));

        // 2. 유효성 검증
        if (!refreshTokenService.validateRefreshToken(refreshToken, member)) {
            throw new RuntimeException("리프레시 토큰이 만료되었거나 유효하지 않습니다.");
        }

        String userId;
        switch (member.getLogintype()) {
            case LOCAL -> userId = localMemberRepository.findByMember(member)
                    .orElseThrow(() -> new RuntimeException("LocalMember 정보 없음"))
                    .getUserId();
            case SOCIAL -> userId = socialMemberRepository.findByMember(member)
                    .orElseThrow(() -> new RuntimeException("SocialMember 정보 없음"))
                    .getUserId();
            default -> throw new IllegalStateException("지원하지 않는 로그인 타입입니다.");
        }

        // 3. 새로운 액세스 토큰 생성
        return jwtUtility.generateToken(
                userId,
                member.getRole()
        );
    }
}