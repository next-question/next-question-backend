package com.buildup.nextQuestion.dto.solving;

import com.buildup.nextQuestion.domain.enums.SolvedType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveHistoryByExamRequest {
    private SolvedType type;
    private String encryptedWorkBookId;
    private List<ExamInfoDTO> info;

}
