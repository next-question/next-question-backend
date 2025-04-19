package com.buildup.nextQuestion.dto.wrongNote;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionsByWrongNoteDTO {
    private String encryptedQuestionId;
    private String encryptedWorkBookId;
    private String name;
    private QuestionType type;
    private String answer;
    private String opt;
    private Timestamp solvedDate;
    private Timestamp recentSolveTime;
}
