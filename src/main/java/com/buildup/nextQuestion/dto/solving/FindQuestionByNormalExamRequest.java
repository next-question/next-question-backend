package com.buildup.nextQuestion.dto.solving;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.buildup.nextQuestion.dto.question.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionByNormalExamRequest {
    private String encryptedWorkBookId;
    private NormalExamOption options;
}