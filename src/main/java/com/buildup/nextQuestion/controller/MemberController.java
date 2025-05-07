package com.buildup.nextQuestion.controller;


import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.dto.member.*;
import com.buildup.nextQuestion.service.LocalMemberService;
import com.buildup.nextQuestion.service.MemberService;
import com.buildup.nextQuestion.service.security.RefreshTokenService;
import com.buildup.nextQuestion.service.security.SecurityService;
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
    private final SecurityService securityService;

    @GetMapping("public/member/refresh")
    public ResponseEntity<?> refreshAccessToken(
            @RequestHeader("Authorization") String refreshToken) throws Exception {
        String newAccessToken = securityService.refreshAccessToken(refreshToken.substring(7));
        return ResponseEntity.ok(newAccessToken);
    }

    @PostMapping("public/member/regist")
    public ResponseEntity<String> register(@RequestBody RegistRequest registDTORequest) {
            LocalMember localMember = localMemberService.register(registDTORequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입에 성공했습니다.");
    }

    @PostMapping("public/member/login/local")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginDTOrequest) {
        LoginResponse response = memberService.login(loginDTOrequest, loginDTOrequest.isKeepLogin());
        return ResponseEntity.ok(response);
    }

    @GetMapping("public/members/search")
    public ResponseEntity<?> findAllMember (
            @RequestHeader("Authorization") String token) throws Exception {
            List<FindMembersResponse> response = memberService.findMembers();
            return ResponseEntity.ok(response);
    }

    @GetMapping("member/attendence/find")
    public ResponseEntity<?> findTodayAttendance (
            @RequestHeader("Authorization") String token) throws Exception {
        AttendanceResponse response = memberService.findTodayAttendance(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("member/attendences/find")
    public ResponseEntity<?> findAllAttendances (
            @RequestHeader("Authorization") String token) throws Exception {
        List<String> response = memberService.findAllAttendances(token);
        return ResponseEntity.ok(response);
    }


}

