package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findAllByMemberId(Long id);
}
