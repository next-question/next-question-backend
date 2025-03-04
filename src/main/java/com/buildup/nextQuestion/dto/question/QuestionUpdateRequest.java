package com.buildup.nextQuestion.dto.question;

import lombok.*;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionUpdateRequest {
    private Long questionId;
    private Boolean wrong;
    private Timestamp recentSolveTime;
}
