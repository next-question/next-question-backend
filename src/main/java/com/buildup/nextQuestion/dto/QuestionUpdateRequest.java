package com.buildup.nextQuestion.dto;

import lombok.*;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class QuestionUpdateRequest {
    private Long questionId;
    private Boolean wrong;
    private Timestamp recentSolveTime;
}
