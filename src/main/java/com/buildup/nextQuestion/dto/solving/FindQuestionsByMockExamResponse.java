package com.buildup.nextQuestion.dto.solving;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionsByMockExamResponse {
    private String encryptedQuestionId;
    private String name;
    private QuestionType type;
    private String answer;
    private String opt;
    private Boolean wrong;
    private Timestamp recentSolveTime;
}
