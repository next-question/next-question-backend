package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.HistoryInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoryInfoRepository extends JpaRepository<HistoryInfo, Long> {
    List<HistoryInfo> findAllByHistoryId(Long historyId);
    List<HistoryInfo> findByWrongIsTrueAndHistoryIn(List<History> histories);
    List<HistoryInfo> findByHistory(History history);
}
