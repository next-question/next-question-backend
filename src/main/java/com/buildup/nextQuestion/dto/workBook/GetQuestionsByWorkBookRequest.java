package com.buildup.nextQuestion.dto.workBook;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetQuestionsByWorkBookRequest {
    private String encryptedWorkBookId;
}
