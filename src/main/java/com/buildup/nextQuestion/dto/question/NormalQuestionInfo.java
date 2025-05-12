package com.buildup.nextQuestion.dto.question;

import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.WorkBook;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NormalQuestionInfo {
    private Question question;
    private String encryptedWorkbookId;
}
