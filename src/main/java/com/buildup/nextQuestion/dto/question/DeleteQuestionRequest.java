package com.buildup.nextQuestion.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteQuestionRequest {
    private String encryptedWorkBookId;
    private List<String> encryptedQuestionIds;
}
