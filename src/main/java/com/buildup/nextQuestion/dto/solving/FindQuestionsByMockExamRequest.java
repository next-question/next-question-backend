package com.buildup.nextQuestion.dto.solving;

import com.buildup.nextQuestion.dto.question.NormalExamOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionsByMockExamRequest {
    private String encryptedWorkBookId;
    private MockExamOption options;
}
