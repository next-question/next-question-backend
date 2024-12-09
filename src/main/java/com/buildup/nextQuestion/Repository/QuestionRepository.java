package com.buildup.nextQuestion.Repository;

import com.buildup.nextQuestion.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE q.workBook.id = :workbookId")
    List<Question> findAllByWorkBook(@Param("workbookId") Long workBook);

}
