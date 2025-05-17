package com.buildup.nextQuestion.dto.wrongNote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindWrongNoteResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<FindWrongNoteDTO> questions;
}
