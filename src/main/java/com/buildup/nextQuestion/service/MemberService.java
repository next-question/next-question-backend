package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.Attendance;
import com.buildup.nextQuestion.domain.LocalMember;
import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.dto.member.*;
import com.buildup.nextQuestion.repository.AttendanceRepository;
import com.buildup.nextQuestion.repository.LocalMemberRepository;
import com.buildup.nextQuestion.repository.MemberRepository;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.buildup.nextQuestion.service.security.RefreshTokenService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final LocalMemberRepository localMemberRepository;
    private final MemberFinder memberFinder;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtility jwtUtility;
    private final MemberRepository memberRepository;
    private final EncryptionService encryptionService;
    private final AttendanceRepository attendanceRepository;
    private final RefreshTokenService refreshTokenService;

    public LoginResponse login(LoginRequest loginDTOrequest, boolean keepLogin) {
        String userId = loginDTOrequest.getUserId();
        String password = loginDTOrequest.getPassword();

        // 1. 유저 조회
        LocalMember localMember = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        Member member = localMember.getMember();

        // 2. 비밀번호 검증
        if (passwordEncoder.matches(password, localMember.getPassword())) {
            // 3. JWT 및 리프레시 토큰 조건 생성
            String accessToken = jwtUtility.generateToken(userId, member.getRole());
            String refreshToken = keepLogin ? refreshTokenService.createRefreshToken(member) : null;

            return new LoginResponse(userId, refreshToken, accessToken, member.getNickname(), member.getRole());
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
        Member member = memberFinder.findMember(userId);

        AttendanceResponse response = new AttendanceResponse();
        response.setHasAttended(false);
        if (attendanceRepository.existsByMemberAndDate(member, today)){
            response.setHasAttended(true);
        }
        return response;
    }

    public List<String> findAllAttendances(String token){
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = memberFinder.findMember(userId);

        List<String> attendances = new ArrayList<>();

        for (Attendance attendance : attendanceRepository.findAllByMember(member)) {
            String requestedAttendance = attendance.getDate().toString();
            attendances.add(requestedAttendance);
        }

        return attendances;
    }

}
