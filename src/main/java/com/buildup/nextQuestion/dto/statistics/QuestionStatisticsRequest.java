package com.buildup.nextQuestion.dto.statistics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuestionStatisticsRequest {
    Integer days;
    Integer threshold;
}