package com.buildup.nextQuestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistDTORequest {
    private String userId;
    private String password;
    private String email;
    private String nickname;
}
