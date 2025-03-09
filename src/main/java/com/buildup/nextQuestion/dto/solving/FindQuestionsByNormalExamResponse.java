package com.buildup.nextQuestion.dto.solving;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindQuestionsByNormalExamResponse {
    private String encryptedQuestionId;
    private String name;
    private QuestionType type;
    private String answer;
    private String opt;
    private Boolean wrong;
    private Timestamp recentSolveTime;

}