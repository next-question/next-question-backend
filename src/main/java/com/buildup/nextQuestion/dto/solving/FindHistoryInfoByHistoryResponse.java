package com.buildup.nextQuestion.dto.solving;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindHistoryInfoByHistoryResponse {
    private String name;
    private QuestionType type;
    private String answer;
    private String opt;
    private Boolean wrong;
}
