package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    void deleteByMemberIdAndQuestionInfoId(Long memberId, Long questionInfoId);
    public List<Question> findAllByMemberId(Long memberId);
}
