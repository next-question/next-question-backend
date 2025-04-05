package com.buildup.nextQuestion.dto.solving;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordAttendanceRequest {
    private String encryptedQuestionId;
    private boolean wrong;
}
