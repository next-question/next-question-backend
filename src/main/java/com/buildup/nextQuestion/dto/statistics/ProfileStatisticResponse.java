package com.buildup.nextQuestion.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ProfileStatisticResponse {
    String nickName;
    String userId;
    Integer averageCorrectRate;
    Integer todaySolvedCount;
    Integer thisMonthSolvedCount;
    Double monthlyAverageSolvedCount;
    Integer steak;
    Integer maxStreak;
    List<DailySolveCountDto> DailySolveCountThisMonth;
}
