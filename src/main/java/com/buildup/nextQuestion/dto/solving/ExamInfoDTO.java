package com.buildup.nextQuestion.dto.solving;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExamInfoDTO {
    private String encryptedQuestionId;
    private Boolean wrong;
}
