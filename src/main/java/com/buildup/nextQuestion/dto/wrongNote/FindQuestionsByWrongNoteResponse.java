package com.buildup.nextQuestion.dto.wrongNote;

import com.buildup.nextQuestion.domain.enums.QuestionType;

import java.sql.Timestamp;

public class FindQuestionByWrongNoteResponse {
    private String encryptedQuestionId;
    private String name;
    private QuestionType type;
    private String answer;
    private String opt;
    private Timestamp createTime;
    private Timestamp recentSolveTime;
}
