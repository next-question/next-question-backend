package com.buildup.nextQuestion.dto.member;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceResponse {
    private LocalDate date;
    private boolean hasAttended;
}
