package com.buildup.nextQuestion.dto.wrongNote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindQuestionsByWrongNoteRequest {
    private Long historyId;
}
