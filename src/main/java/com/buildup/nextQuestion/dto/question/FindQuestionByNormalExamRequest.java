package com.buildup.nextQuestion.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionByNormalExamRequest {
    private String encryptedWorkBookId;
    private NormalExamOption options;
}
