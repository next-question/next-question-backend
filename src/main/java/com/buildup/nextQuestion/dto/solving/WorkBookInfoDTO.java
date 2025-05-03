package com.buildup.nextQuestion.dto.solving;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkBookInfoDTO {
    private String encryptedWorkBookId;
    private List<ExamInfoDTO> info;
}
