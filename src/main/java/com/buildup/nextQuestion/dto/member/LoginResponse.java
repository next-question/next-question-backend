package com.buildup.nextQuestion.dto.member;

import com.buildup.nextQuestion.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String refreshToken;
    private String accessToken;
    private String nickname;
    private Role role;
}
