package com.buildup.nextQuestion.controller;


import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.dto.member.LoginRequest;
import com.buildup.nextQuestion.dto.member.LoginResponse;
import com.buildup.nextQuestion.dto.member.RegistRequest;
import com.buildup.nextQuestion.service.LocalMemberService;
import com.buildup.nextQuestion.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final LocalMemberService localMemberService;
    private final MemberService memberService;

    @CrossOrigin
    @PostMapping("public/member/regist")
    public ResponseEntity<String> register(@RequestBody RegistRequest registDTORequest) {
            LocalMember localMember = localMemberService.register(registDTORequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입에 성공했습니다.");
    }

    @CrossOrigin
    @PostMapping("public/member/login/local")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginDTOrequest) {
            LoginResponse response = memberService.login(loginDTOrequest);
            return ResponseEntity.ok(response);
    }
}