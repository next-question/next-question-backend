package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.WorkBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkBookRepository extends JpaRepository<WorkBook, Long> {
    List<WorkBook> findAllByMemberId(Long id);
    List<WorkBook> findByNameAndMemberId(String name, Long Id);
    boolean existsByIdAndMemberId(Long id, Long memberId);
}
