package com.buildup.nextQuestion.dto.question;

import com.buildup.nextQuestion.dto.solving.FindQuestionsByTypeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveQuestionResponse {
    FindQuestionsByTypeResponse workbookInfo;
}
