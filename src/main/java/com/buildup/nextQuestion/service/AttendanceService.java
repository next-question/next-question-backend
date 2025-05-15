package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.Attendance;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.Statistics;
import com.buildup.nextQuestion.dto.solving.RecordAttendanceRequest;
import com.buildup.nextQuestion.repository.AttendanceRepository;
import com.buildup.nextQuestion.repository.QuestionRepository;
import com.buildup.nextQuestion.repository.StatisticsRepository;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class AttendanceService {
    JwtUtility jwtUtility;
    AttendanceRepository attendanceRepository;
    EncryptionService encryptionService;
    QuestionRepository questionRepository;
    MemberFinder memberFinder;
    StatisticsRepository statisticsRepository;


    @Transactional
    public void recordAttendance(String token, List<RecordAttendanceRequest> requests) throws Exception {
        LocalDate today = LocalDate.now();

        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = memberFinder.findMember(userId);

        // 이미 출석 기록이 있는지 확인
        boolean alreadyChecked = attendanceRepository.existsByMemberAndDate(member, today);
        if (alreadyChecked){
            throw new IllegalArgumentException("이미 출석처리 되었습니다.");
        }

        if (requests.size() != 3){
            throw new IllegalArgumentException("출석 인증이 처리되지 않았습니다.");
        }
        // 문제 풀이 처리
        for (RecordAttendanceRequest request : requests) {
            Long questionId = encryptionService.decryptPrimaryKey(request.getEncryptedQuestionId());
            Question question = questionRepository.findById(questionId).get();
            question.setWrong(request.isWrong());
            question.setRecentSolveTime(new Timestamp(System.currentTimeMillis()));
        }

        // 출석 기록 저장
        Attendance attendance = new Attendance();
        attendance.setMember(member);
        attendance.setDate(today);
        attendance.setCheckedTime(new Timestamp(System.currentTimeMillis()));
        attendanceRepository.save(attendance);

        updateStreak(member, today);
        updateMaxStreak(member);
    }

    private void updateStreak(Member member, LocalDate today) {
        Statistics stats = statisticsRepository.findByMember(member)
                .orElseThrow(() -> new IllegalStateException("통계 정보가 없습니다."));

        LocalDate yesterday = today.minusDays(1);
        boolean attendedYesterday = attendanceRepository.existsByMemberAndDate(member, yesterday);

        if (attendedYesterday) {
            stats.setStreak(stats.getStreak() + 1);
        } else {
            stats.setStreak(1);
        }

        statisticsRepository.save(stats);
    }

    private void updateMaxStreak(Member member) {
        Statistics stats = statisticsRepository.findByMember(member)
                .orElseThrow(() -> new IllegalStateException("통계 정보가 없습니다."));

        if (stats.getStreak() > stats.getMaxStreak()) {
            stats.setMaxStreak(stats.getStreak());
            statisticsRepository.save(stats);
        }
    }


}
