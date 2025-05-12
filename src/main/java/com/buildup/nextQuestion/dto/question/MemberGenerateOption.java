package com.buildup.nextQuestion.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberGenerateOption {
    private Integer questionCount;
    private Boolean multiple;
    private Boolean ox;
    private Boolean blank;
}
