package com.buildup.nextQuestion.controller;


import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.dto.RegistDTORequest;
import com.buildup.nextQuestion.service.LocalMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final LocalMemberService localMemberService;

    @PostMapping("public/member/regist")
    public ResponseEntity<?> register(@RequestBody RegistDTORequest registDTORequest) {
        try {
            // 회원가입 처리
            LocalMember localMember = localMemberService.register(registDTORequest);

            // 회원가입 성공 응답
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공. 회원 ID: " + localMember.getId());

        } catch (IllegalArgumentException e) {
            // 중복된 아이디 또는 이메일
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            // 기타 서버 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }


}

