package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.QuestionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    void deleteByMemberIdAndQuestionInfoId(Long memberId, Long questionInfoId);
    List<Question> findAllByMemberId(Long memberId);
    Optional<Question> findByMemberIdAndQuestionInfoId(Long memberId, Long questionInfoId);
    Optional<Question> findByMemberIdAndQuestionInfoIdAndDelFalse(Long memberId, Long questionInfoId);
}
