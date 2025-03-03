package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
