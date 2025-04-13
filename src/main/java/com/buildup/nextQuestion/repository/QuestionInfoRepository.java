package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.QuestionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface QuestionInfoRepository extends JpaRepository<QuestionInfo, Long> {

}