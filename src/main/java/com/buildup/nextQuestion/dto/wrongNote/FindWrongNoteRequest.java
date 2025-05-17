package com.buildup.nextQuestion.dto.wrongNote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindWrongNoteRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodType;

}
