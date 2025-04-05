package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.Attendance;
import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.dto.member.*;
import com.buildup.nextQuestion.repository.AttendanceRepository;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.MemberRepository;
import com.buildup.nextQuestion.utility.JwtUtility;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.processing.Find;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly=true)
@RequiredArgsConstructor
public class MemberService {

    private final LocalMemberRepository localMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtility jwtUtility;
    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;
    private final AttendanceRepository attendanceRepository;

    public LoginResponse login(LoginRequest loginDTOrequest) {
        String userId = loginDTOrequest.getUserId();
        String password = loginDTOrequest.getPassword();
        // 1. userId로 회원 조회
        LocalMember localMember = localMemberRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        Member member = localMember.getMember();
        // 2. 비밀번호 검증
        if (passwordEncoder.matches(password, localMember.getPassword())) {
            LoginResponse loginDTOresponse = new LoginResponse(jwtUtility.generateToken(userId, member.getRole()), member.getNickname(), member.getRole());
            // 3. JWT 토큰 생성 후 반환
            return loginDTOresponse;
        } else {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public List<FindMembersResponse> findMembers() throws Exception {
        List<Member> members = memberRepository.findAll();
        List<FindMembersResponse> response = new ArrayList<>();
        for (Member member : members) {
            FindMembersResponse memberInfo = new FindMembersResponse();
            memberInfo.setEncryptedMemberId(encryptionService.encryptPrimaryKey(member.getId()));
            memberInfo.setNickname(member.getNickname());
            memberInfo.setLoginType(member.getLogintype());
            response.add(memberInfo);
        }
        return response;
    }

    public AttendanceResponse findTodayAttendance(String token) throws Exception {
        LocalDate today = LocalDate.now();

        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        AttendanceResponse response = new AttendanceResponse();
        response.setDate(today);
        response.setHasAttended(false);
        if (attendanceRepository.existsByMemberAndDate(member, today)){
            response.setHasAttended(true);
        }
        return response;
    }

    public FindTodayAttendanceResponse findAllAttendances(String token){
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        List<AttendanceResponse> attendances = new ArrayList<>();

        for (Attendance attendance : attendanceRepository.findAllByMember(member)) {
            AttendanceResponse requestedAttendance = new AttendanceResponse();
            requestedAttendance.setDate(attendance.getDate());
            requestedAttendance.setHasAttended(true);
            attendances.add(requestedAttendance);
        }

        LocalMember localMember = localMemberRepository.findByUserId(userId).get();
        FindTodayAttendanceResponse response = new FindTodayAttendanceResponse();
        response.setAttendances(attendances);
        response.setCreateDate(localMember.getCreateDate());
        return response;
    }

}
