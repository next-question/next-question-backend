package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.WorkBookInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkBookInfoRepository extends JpaRepository<WorkBookInfo, Long> {
    List<WorkBookInfo> findAllByMemberId(Long id);
}
