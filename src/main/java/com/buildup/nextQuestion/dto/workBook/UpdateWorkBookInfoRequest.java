package com.buildup.nextQuestion.dto.workBook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateWorkBookInfoRequest {
    private String encryptedWorkBookInfoId;
    private String name;
}
