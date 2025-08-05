package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.Member;
import com.buildup.nextQuestion.domain.SocialMember;
import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.domain.enums.SocialType;
import com.buildup.nextQuestion.dto.member.LoginResponse;
import com.buildup.nextQuestion.dto.google.GoogleInfResponse;
import com.buildup.nextQuestion.dto.google.GoogleRequest;
import com.buildup.nextQuestion.dto.google.GoogleResponse;
import com.buildup.nextQuestion.dto.member.SocialRegistRequest;
import com.buildup.nextQuestion.repository.MemberRepository;
import com.buildup.nextQuestion.repository.SocialMemberRepository;
import com.buildup.nextQuestion.service.security.RefreshTokenService;
import com.buildup.nextQuestion.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialMemberService {
    private final SocialMemberRepository socialMemberRepository;
    private final JwtUtility jwtUtility;
    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;
    private final StatisticsService statisticsService;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.client.pw}")
    private String googleClientPw;

    public String loginUrlGoogle(){
        String reqUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + googleClientId
                + "&redirect_uri=http://localhost:3000/google/callback&response_type=code&scope=email%20profile%20openid&access_type=offline&prompt=consent";
        return reqUrl;
    }

    public String authGoogle(String authCode) {
        RestTemplate restTemplate = new RestTemplate();
        GoogleRequest googleOAuthRequestParam = GoogleRequest
                .builder()
                .clientId(googleClientId)
                .clientSecret(googleClientPw)
                .code(authCode)
                .redirectUri("http://localhost:3000/google/callback")
                .grantType("authorization_code").build();
        //해당 코드를 이용해 인증을 받아옴
        ResponseEntity<GoogleResponse> resultEntity = restTemplate.postForEntity("https://oauth2.googleapis.com/token",
                googleOAuthRequestParam, GoogleResponse.class);
        //인증 토큰 활용해 구글에서 사용할 jwtToken 발급
        String jwtToken = resultEntity.getBody().getId_token();
        Map<String, String> map = new HashMap<>();
        map.put("id_token", jwtToken);
        //jwtToken을 이용해 유저 정보 찾아옴
        ResponseEntity<GoogleInfResponse> resultEntity2 = restTemplate.postForEntity("https://oauth2.googleapis.com/tokeninfo",
                map, GoogleInfResponse.class);

        String snsId = resultEntity2.getBody().getEmail();
        return snsId;
    }

    public LoginResponse loginGoogle(String snsId, boolean keepLogin) {
        SocialMember socialMember = socialMemberRepository.findBySnsId(snsId).orElseThrow(() -> new IllegalArgumentException("해당 ID가 존재하지 않습니다."));
        Member member = socialMember.getMember();
        String accessToken = jwtUtility.generateToken(snsId, member.getRole());
        String refreshToken = keepLogin ? refreshTokenService.createRefreshToken(member) : null;
        LoginResponse loginDTOresponse = new LoginResponse(snsId, refreshToken, accessToken, member.getNickname(), member.getRole());
        // 3. JWT 토큰 생성 후 반환
        return loginDTOresponse;
    }

    @Transactional
    public void registerGoogle(SocialRegistRequest registDTORequest) {
        String userId = registDTORequest.getUserId();
        String nickname = registDTORequest.getNickname();
        // 중복 체크
        if (memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("동일한 별명을 사용할 수 없습니다.");
        }

        // Member 객체 생성
        Member member = new Member(nickname, LoginType.SOCIAL);
        memberRepository.save(member);

        // 통계 초기화
        statisticsService.initStatistics(member);

        SocialMember socialMember = new SocialMember(userId, SocialType.GOOGLE, member);
        socialMemberRepository.save(socialMember);
    }

    public String decodeCode(String code) throws UnsupportedEncodingException {
        return URLDecoder.decode(code, "UTF-8");
    }
}
