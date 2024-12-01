package com.buildup.nextQuestion.Repository;

import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.WorkBook;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    public List<Question> findAllByWorkBookId(Long workBookId);

}