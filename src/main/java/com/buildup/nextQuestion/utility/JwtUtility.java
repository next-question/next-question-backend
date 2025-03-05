package com.buildup.nextQuestion.utility;

import com.buildup.nextQuestion.domain.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtUtility {

    private final SecretKey secretKey;

    public JwtUtility() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    private static final long EXPIRATION_TIME = 1000L * 60 * 60; // 1시간

    public String generateToken(String userId, Role role) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .addClaims(Map.of("role", role.name()))
                .signWith(secretKey)
                .compact();
    }

    //토큰 검증 및 파싱
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token.replace("Bearer ", "")) // "Bearer " 제거
                .getBody();

        return claims.getSubject();
    }


    //토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        return validateToken(token).getExpiration().before(new Date());
    }
}
