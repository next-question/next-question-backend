package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.History;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository extends JpaRepository<History, Long> {
}
