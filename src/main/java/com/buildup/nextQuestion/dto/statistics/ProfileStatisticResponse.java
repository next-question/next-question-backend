package com.buildup.nextQuestion.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProfileStatisticResponse {
    Integer todaySolvedCount;
    Integer thisMonthSolvedCount;
    Double monthlyAverageSolvedCount;
    Integer steak;
    Integer maxStreak;
}
