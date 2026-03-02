package com.notecurve.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;
import com.notecurve.auth.security.JwtTokenProvider;
import com.notecurve.auth.domain.RefreshToken;
import com.notecurve.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 로그인 시 액세스 + 리프레시 토큰 둘 다 반환
    @Transactional
    public TokenPair login(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(loginId);
        String refreshToken = createAndSaveRefreshToken(loginId);

        return new TokenPair(accessToken, refreshToken);
    }

    // 리프레시 토큰으로 액세스 토큰 재발급
    @Transactional
    public TokenPair refresh(String refreshToken) {
        // JWT 자체 유효성 검사 먼저
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // DB에서 토큰 존재 여부 확인
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

        // DB 만료일 확인 (이중 체크)
        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
        }

        String loginId = stored.getLoginId();

        // 기존 리프레시 토큰 삭제 후 새로 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(loginId);
        String newRefreshToken = createAndSaveRefreshToken(loginId);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    // 로그아웃 시 DB에서 리프레시 토큰 삭제
    @Transactional
    public void logout(String loginId) {
        refreshTokenRepository.deleteByLoginId(loginId);
    }

    @Transactional
    private String createAndSaveRefreshToken(String loginId) {
        // 기존 토큰 삭제 (중복 방지)
        refreshTokenRepository.deleteByLoginId(loginId);

        String token = jwtTokenProvider.generateRefreshToken(loginId);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);

        refreshTokenRepository.save(new RefreshToken(token, loginId, expiresAt));
        return token;
    }

    // 토큰 쌍 반환용 record
    public record TokenPair(String accessToken, String refreshToken) {}
}
