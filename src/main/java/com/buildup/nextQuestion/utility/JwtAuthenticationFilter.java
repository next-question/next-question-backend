package com.buildup.nextQuestion.utility;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtility jwtUtility;

    public JwtAuthenticationFilter(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/public/");  // ✅ /public/** 경로는 필터 제외
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        String token = extractToken(request);

        if (token == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 제공되지 않았습니다.");
            return;
        }

        try {
            Claims claims = jwtUtility.validateToken(token); // 🔥 토큰 검증 및 파싱 수행

            if (jwtUtility.isTokenExpired(token)) { // 🔥 만료 여부 확인
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 만료되었습니다.");
                return;
            }

            String role = claims.get("role", String.class);

            if (role == null) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "유효한 역할이 없습니다.");
                return;
            }

            // 🔹 사용자 인증 정보 설정 (ADMIN, USER에 따라)
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 🔹 관리자 전용 API 접근 권한 확인 (일반 사용자가 ADMIN API 접근 시 차단)
            if (requestURI.startsWith("/account/admin")) {
                if (!role.equals("ADMIN")) {
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요합니다.");
                    return;
                }
            }

        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 만료되었습니다.");
            return;
        } catch (JwtException e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.");
            return;
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT 인증 과정에서 오류가 발생했습니다.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
