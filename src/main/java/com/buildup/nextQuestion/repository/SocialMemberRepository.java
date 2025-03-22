package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.SocialMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialMemberRepository extends JpaRepository<SocialMember, Long> {
    boolean existsBySnsId(String snsId);
    Optional<SocialMember> findBySnsId(String snsId);
}
