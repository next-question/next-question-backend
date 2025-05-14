package com.buildup.nextQuestion.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSolveStats {
    Integer totalAttempts;
    Integer CorrectCount;
    Integer wrongCount;
}

