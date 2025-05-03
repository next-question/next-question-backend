package com.buildup.nextQuestion.dto.wrongNote;

import java.util.List;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupedWorkBookDTO {
    private String mainWorkBookName;
    private String WorkBookCount;
    private String QuestionCount;
    private String HistoryId;
    private List<WrongNoteWorkBookDTO> workBooks;
}
