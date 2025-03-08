package com.buildup.nextQuestion.dto.member;

import com.buildup.nextQuestion.domain.enums.LoginType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindMembersResponse {
    private String encryptedMemberId;
    private String nickname;
    private LoginType loginType;
}
