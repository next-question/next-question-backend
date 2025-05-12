package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalMemberRepository extends JpaRepository<LocalMember, Long> {
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    Optional<LocalMember> findByUserId(String userId);
    Optional<LocalMember> findByMember(Member member);
}
