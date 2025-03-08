package com.buildup.nextQuestion.controller;


import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.dto.member.FindMembersResponse;
import com.buildup.nextQuestion.dto.member.LoginRequest;
import com.buildup.nextQuestion.dto.member.LoginResponse;
import com.buildup.nextQuestion.dto.member.RegistRequest;
import com.buildup.nextQuestion.service.LocalMemberService;
import com.buildup.nextQuestion.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final LocalMemberService localMemberService;
    private final MemberService memberService;

    @PostMapping("public/member/regist")
    public ResponseEntity<String> register(@RequestBody RegistRequest registDTORequest) {
        try {
            // 회원가입 처리
            LocalMember localMember = localMemberService.register(registDTORequest);

            // 회원가입 성공 응답
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입에 성공했습니다.");

        } catch (IllegalArgumentException e) {
            // 중복된 아이디 또는 이메일
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            // 기타 서버 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    @PostMapping("public/member/login/local")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginDTOrequest) {
        try {
            LoginResponse response = memberService.login(loginDTOrequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("public/members/search")
    public ResponseEntity<?> findAllMember(
            @RequestHeader("Authorization") String token) {
        try {
            List<FindMembersResponse> response = memberService.findMembers();
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }


}

