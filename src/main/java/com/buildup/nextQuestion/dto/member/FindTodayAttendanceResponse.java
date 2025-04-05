package com.buildup.nextQuestion.dto.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindTodayAttendanceResponse {
    private LocalDate createDate;
    private List<String> attendances;
}
