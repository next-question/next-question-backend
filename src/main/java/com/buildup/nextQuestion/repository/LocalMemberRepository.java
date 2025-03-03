package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.LocalMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalMemberRepository extends JpaRepository<LocalMember, Long> {
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
}
