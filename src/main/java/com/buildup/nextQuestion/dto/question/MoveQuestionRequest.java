package com.buildup.nextQuestion.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveQuestionRequest {
    private String encryptedSourceWorkbookId;
    private String encryptedTargetWorkbookId;
    private List<String> encryptedQuestionInfoIds;

}
