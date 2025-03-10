package com.buildup.nextQuestion.dto.workBook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetWorkBookResponse {
    private String encryptedWorkBookId;
    private String name;
    private Timestamp recentSolvedDate;
    private int totalQuestion;
}
