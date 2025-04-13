package com.buildup.nextQuestion.dto.solving;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.buildup.nextQuestion.dto.question.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionsByNormalExamRequest {
    private List<String> encryptedWorkBookIds;
    private NormalExamOption options;
}