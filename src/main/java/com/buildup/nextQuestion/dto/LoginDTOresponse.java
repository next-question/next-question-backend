package com.buildup.nextQuestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTOresponse {
    private String accessToken;
    private String nickname;
}
