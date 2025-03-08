package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.HistoryInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryInfoRepository extends JpaRepository<HistoryInfo, Long> {
}
