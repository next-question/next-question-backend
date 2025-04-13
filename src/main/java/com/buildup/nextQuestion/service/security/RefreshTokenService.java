package com.buildup.nextQuestion.service.security;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    private final MemberRepository memberRepository;
    private static final long REFRESH_TOKEN_EXPIRY_MS = 14 * 24 * 3600 * 1000L; // 2주

    // 리프레쉬 토큰 생성 및 저장
    @Transactional
    public String createRefreshToken(Member member) {
        try {
            String refreshToken = UUID.randomUUID().toString();
            member.setRefreshToken(refreshToken);
            member.setExpiryDate(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS));
            memberRepository.save(member);
            return refreshToken;
        } catch (Exception e) {
            throw new RuntimeException("리프레시 토큰 생성 중 오류 발생", e);
        }
    }

    // 리프레쉬 토큰 갱신
    public void renewRefreshToken(Member member) {
        try {
            String newRefreshToken = UUID.randomUUID().toString();
            member.setRefreshToken(newRefreshToken);
            member.setExpiryDate(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS));
            memberRepository.save(member);
        } catch (Exception e) {
            throw new RuntimeException("리프레시 토큰 갱신 중 오류 발생", e);
        }
    }

    // 리프레쉬 토큰 검증
    public boolean validateRefreshToken(String refreshToken, Member member) {
        try {
            Date now = new Date();
            if (member.getExpiryDate() == null || member.getExpiryDate().before(now)) {
                return false;
            }
            return refreshToken != null && refreshToken.equals(member.getRefreshToken());
        } catch (Exception e) {
            // 로그를 찍거나 필요 시 추가 처리
            return false;
        }
    }

    // 리프레쉬 토큰 만료
    public void expireRefreshToken(Member member) {
        try {
            member.setRefreshToken(null);
            member.setExpiryDate(null);
            memberRepository.save(member);
        } catch (Exception e) {
            throw new RuntimeException("리프레시 토큰 만료 처리 중 오류 발생", e);
        }
    }
}