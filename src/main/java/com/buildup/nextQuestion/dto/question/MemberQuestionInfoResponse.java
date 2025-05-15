package com.buildup.nextQuestion.dto.question;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberQuestionInfoResponse {
    private String encryptedQuestionId;
    private String name;
    private QuestionType type;
    private String answer;
    private String opt;
    private LocalTime createTime;
}
