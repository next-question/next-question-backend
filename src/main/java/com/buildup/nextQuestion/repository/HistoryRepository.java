package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.History;
import com.buildup.nextQuestion.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findAllByMemberId(Long id);
    List<History> findAllByMemberIdAndSolvedDateBetween(Long memberId, Timestamp startDate, Timestamp endDate);
    List<History> findByMemberOrderBySolvedDateAsc(Member member);
    List<History> findByMemberIdAndSolvedDateAfterAndTypeIn(Long memberId, Timestamp afterDate, List<SolvedType> types);
    List<History> findByMemberIdAndSolvedDateAfter(Long memberId, Timestamp fromDate);
}
