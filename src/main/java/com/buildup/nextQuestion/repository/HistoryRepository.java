package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.enums.SolvedType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findAllByMemberId(Long id);
    List<History> findAllByMemberIdAndSolvedDateBetween(Long memberId, Timestamp startDate, Timestamp endDate);
    List<History> findByMemberOrderBySolvedDateAsc(Member member);
}
