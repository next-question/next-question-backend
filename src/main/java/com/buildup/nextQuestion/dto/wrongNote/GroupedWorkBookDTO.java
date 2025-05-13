package com.buildup.nextQuestion.dto.wrongNote;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.List;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupedWorkBookDTO {
    private String mainWorkBookName;
    private int WorkBookCount;
    private int QuestionCount;
    private String HistoryId;
    private Timestamp SolvedDate;
    private List<WrongNoteWorkBookDTO> workBooks;
}
