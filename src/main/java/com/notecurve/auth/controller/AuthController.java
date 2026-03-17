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
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            AuthService.TokenPair tokens = authService.login(loginRequest.loginId(), loginRequest.password());
            User user = userService.findByLoginId(loginRequest.loginId());

            ResponseCookie accessCookie = createTokenCookie("token", tokens.accessToken());
            ResponseCookie refreshCookie = createTokenCookie("refresh_token", tokens.refreshToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(new LoginResponse(
                        "로그인 성공",
                        tokens.accessToken(),
                        user.getLoginId(),
                        user.getName(),
                        user.getId(),
                        user.getProfileImage(),
                        user.getRole().name()
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // 액세스 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(HttpServletRequest request) {
        String refreshToken = getTokenFromCookies(request, "refresh_token");
        if (refreshToken == null) {
            return ResponseEntity.status(401).body("리프레시 토큰이 없습니다.");
        }

        try {
            AuthService.TokenPair tokens = authService.refresh(refreshToken);

            ResponseCookie accessCookie = createTokenCookie("token", tokens.accessToken());
            ResponseCookie refreshCookie = createTokenCookie("refresh_token", tokens.refreshToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body("토큰이 재발급되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) {
            return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
        }

        authService.logout(user.getLoginId()); // DB에서 리프레시 토큰 삭제

        ResponseCookie deleteAccess = deleteTokenCookie("token");
        ResponseCookie deleteRefresh = deleteTokenCookie("refresh_token");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteAccess.toString())
                .header(HttpHeaders.SET_COOKIE, deleteRefresh.toString())
                .body("로그아웃 완료");
    }

    // 로그인 상태 체크
    @GetMapping("/check")
    public ResponseEntity<LoginResponse> checkLogin(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) return ResponseEntity.status(401).build();

        String token = getTokenFromCookies(request, "token");
        return ResponseEntity.ok(new LoginResponse("로그인 상태", token, user.getLoginId(), user.getName(), user.getId(), user.getProfileImage(), user.getRole().name()));
    }

    // 쿠키 생성
    private ResponseCookie createTokenCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .build();
    }

    // 쿠키 삭제
    private ResponseCookie deleteTokenCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    // 쿠키에서 토큰 추출
    private String getTokenFromCookies(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    // 요청에서 토큰 기반 사용자 조회
    private User getUserFromRequest(HttpServletRequest request) {
        String token = getTokenFromCookies(request, "token");
        if (token == null || !jwtTokenProvider.validateToken(token)) return null;
        String loginId = jwtTokenProvider.getLoginIdFromToken(token);
        return userService.findByLoginId(loginId);
    }
}
