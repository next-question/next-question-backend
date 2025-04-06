package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByNickname(String nickname);
    Optional<Member> findByRefreshToken(String refreshToken);
}

