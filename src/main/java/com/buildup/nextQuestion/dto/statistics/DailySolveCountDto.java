package com.buildup.nextQuestion.dto.statistics;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class DailySolveCountDto {
    private LocalDate date;
    private int solveCount;
}
