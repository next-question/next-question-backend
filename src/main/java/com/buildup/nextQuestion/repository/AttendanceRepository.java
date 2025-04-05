package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.Attendance;
import com.buildup.nextQuestion.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByMemberAndDate(Member member, LocalDate date);

    List<Attendance> findAllByMember(Member member);
}
