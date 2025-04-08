package com.buildup.nextQuestion.support;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.SocialMember;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.SocialMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberFinder {
    private final LocalMemberRepository localMemberRepository;
    private final SocialMemberRepository socialMemberRepository;

    public Member findMember(String userId) {
        return localMemberRepository.findByUserId(userId)
                .map(LocalMember::getMember)
                .orElseGet(() -> socialMemberRepository.findBySnsId(userId)
                        .map(SocialMember::getMember)
                        .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                );
    }

}
