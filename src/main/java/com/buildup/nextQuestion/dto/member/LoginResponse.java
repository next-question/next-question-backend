package com.buildup.nextQuestion.dto.member;

import com.buildup.nextQuestion.domain.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String snsId;
    private String refreshToken;
    private String accessToken;
    private String nickname;
    private Role role;
}
