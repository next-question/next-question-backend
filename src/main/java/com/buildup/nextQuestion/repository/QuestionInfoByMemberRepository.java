package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.QuestionInfoByMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionInfoByMemberRepository  extends JpaRepository<QuestionInfoByMember, Long> {
    public List<QuestionInfoByMember> findAllByMemberId(Long memberId);
}
