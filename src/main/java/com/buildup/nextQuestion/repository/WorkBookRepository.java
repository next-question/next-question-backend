package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.WorkBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkBookRepository extends JpaRepository<WorkBook, Long> {

    void deleteAllByWorkBookInfoId(Long id);
    void deleteByWorkBookInfoIdAndQuestionId(Long workBookInfoId, Long questionId);
    Optional<WorkBook> findByWorkBookInfoIdAndQuestionId(Long workBookInfoId, Long questionId);

    boolean existsByWorkBookInfoIdAndQuestionId(Long targetWorkbookId, Long id);

    List<WorkBook> findAllByWorkBookInfoId(Long decryptedId);
}
