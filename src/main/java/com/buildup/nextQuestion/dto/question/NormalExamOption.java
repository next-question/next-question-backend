package com.buildup.nextQuestion.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NormalExamOption {
    private int count;
    private boolean random;
    private boolean ox;
    private boolean multiple;
    private boolean blank;
}