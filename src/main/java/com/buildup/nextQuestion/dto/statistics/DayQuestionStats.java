package com.buildup.nextQuestion.dto.statistics;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DayQuestionStats {
    private String day;
    private int total;
    private int correct;
}
