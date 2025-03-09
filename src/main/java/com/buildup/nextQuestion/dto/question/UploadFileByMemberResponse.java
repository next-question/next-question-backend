package com.buildup.nextQuestion.dto.question;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileByMemberResponse {
    private String encryptedQuestionInfoId;
    private String name;
    private QuestionType type;
    private String answer;
    private String opt;
}
