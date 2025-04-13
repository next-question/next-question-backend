package com.buildup.nextQuestion.dto.solving;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionsByTypeResponse {
    private Integer multipleChoice;
    private Integer ox;
    private Integer fillInTheBlank;


}
