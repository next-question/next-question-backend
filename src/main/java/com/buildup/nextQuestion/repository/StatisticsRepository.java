package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatisticsRepository extends JpaRepository<Statistics, Long> {
    Optional<Statistics> findByMember(Member member);
}
