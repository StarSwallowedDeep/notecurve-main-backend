package com.notecurve.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.notecurve.auth.dto.LoginRequest;
import com.notecurve.auth.dto.LoginResponse;
import com.notecurve.auth.security.JwtTokenProvider;
import com.notecurve.auth.service.AuthService;
import com.notecurve.user.domain.User;
import com.notecurve.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        // 로그인 검증 및 토큰 발급
        String token = authService.login(loginRequest.loginId(), loginRequest.password());

        // 사용자 정보 조회
        User user = userService.findByLoginId(loginRequest.loginId());

        // 쿠키 생성
        ResponseCookie cookie = createTokenCookie(token, 3600);

        // 응답 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse("로그인 성공", token, user.getLoginId(), user.getName(), user.getId()));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) {
            return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
        }

        ResponseCookie deleteCookie = createTokenCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("로그아웃 완료");
    }

    // 로그인 상태 체크
    @GetMapping("/check")
    public ResponseEntity<LoginResponse> checkLogin(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) return ResponseEntity.status(401).build();

        String token = getTokenFromCookies(request);
        return ResponseEntity.ok(new LoginResponse("로그인 상태", token, user.getLoginId(), user.getName(), user.getId()));
    }

    // 쿠키 생성/삭제
    private ResponseCookie createTokenCookie(String token, long maxAge) {
        return ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(true) // 운영 환경에서는 true
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }

    // 쿠키에서 토큰 추출
    private String getTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("token".equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    // 요청에서 토큰 기반 사용자 조회
    private User getUserFromRequest(HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) return null;
        String loginId = jwtTokenProvider.getLoginIdFromToken(token);
        return userService.findByLoginId(loginId);
    }
}
