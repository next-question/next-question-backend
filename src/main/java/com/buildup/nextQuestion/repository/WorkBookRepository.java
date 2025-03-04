package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.WorkBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkBookRepository extends JpaRepository<WorkBook, Long> {

    void deleteAllByWorkBookInfoId(Long id);
}
