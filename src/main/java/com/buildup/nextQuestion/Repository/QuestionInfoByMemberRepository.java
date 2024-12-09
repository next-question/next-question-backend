package com.buildup.nextQuestion.Repository;

import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.QuestionInfoByMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionInfoByMemberRepository  extends JpaRepository<QuestionInfoByMember, Long> {
}
