package com.buildup.nextQuestion.config;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (memberRepository.count() == 0) { // 기존 데이터가 없을 때만 추가
            Member member = new Member("testMember", LoginType.SOCIAL);
            memberRepository.save(member);
        }
    }
}
