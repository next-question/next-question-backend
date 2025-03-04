package com.buildup.nextQuestion.dto.workBook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetWorkBookInfoResponse {
    private String encryptedWorkBookInfoId;
    private String name;
}
