package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.QuestionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionInfoRepository extends JpaRepository<QuestionInfo, Long> {



}
