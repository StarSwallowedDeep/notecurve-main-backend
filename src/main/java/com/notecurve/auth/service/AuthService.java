package com.notecurve.auth.service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;
import com.notecurve.auth.security.JwtTokenProvider;
import com.notecurve.auth.domain.RefreshToken;
import com.notecurve.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 1;

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

        // Redis에서 먼저 확인
        String loginId = getLoginIdFromRedis(refreshToken);

        // Redis에 없으면 DB에서 확인 (Fallback)
        if (loginId == null) {
            log.warn("Redis에 리프레시 토큰 없음 - DB에서 조회: {}", refreshToken);
            // DB에서 토큰 존재 여부 확인
            RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

            // DB 만료일 확인 (이중 체크)
            if (stored.isExpired()) {
                refreshTokenRepository.delete(stored);
                throw new IllegalArgumentException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
            }

            loginId = stored.getLoginId();

            // DB에서 찾았으면 Redis에 다시 저장 (Redis 복구 시)
            saveToRedis(refreshToken, loginId);
        }

        // 기존 리프레시 토큰 삭제 후 새로 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(loginId);
        String newRefreshToken = createAndSaveRefreshToken(loginId);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    // 로그아웃 시 DB에서 리프레시 토큰 삭제
    @Transactional
    public void logout(String loginId) {
        refreshTokenRepository.deleteByLoginId(loginId);
        deleteFromRedis(loginId);
    }

    @Transactional
    private String createAndSaveRefreshToken(String loginId) {
        // 기존 토큰 삭제 (중복 방지)
        refreshTokenRepository.deleteByLoginId(loginId);
        deleteFromRedis(loginId);

        String token = jwtTokenProvider.generateRefreshToken(loginId);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);

        refreshTokenRepository.save(new RefreshToken(token, loginId, expiresAt));
        saveToRedis(token, loginId);

        return token;
    }

    // ========== Redis 헬퍼 메서드 ==========

    // Redis에 저장 - 두 가지 키로 저장
    private void saveToRedis(String token, String loginId) {
        try {
            // 토큰 → loginId 조회용
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + "token:" + token,
                    loginId,
                    REFRESH_TOKEN_EXPIRE_DAYS,
                    TimeUnit.DAYS
            );
            // loginId → 토큰 삭제용
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + "loginId:" + loginId,
                    token,
                    REFRESH_TOKEN_EXPIRE_DAYS,
                    TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.warn("Redis 저장 실패 - DB로 fallback: {}", e.getMessage());
        }
    }

    // Redis에서 loginId 조회
    private String getLoginIdFromRedis(String token) {
        try {
            return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + "token:" + token);
        } catch (Exception e) {
            log.warn("Redis 조회 실패 - DB로 fallback: {}", e.getMessage());
            return null;
        }
    }

    // Redis에서 loginId 기준으로 토큰 삭제
    private void deleteFromRedis(String loginId) {
        try {
            // loginId로 기존 토큰 조회
            String oldToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + "loginId:" + loginId);
            if (oldToken != null) {
                // 토큰 키 삭제
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + "token:" + oldToken);
            }
            // loginId 키 삭제
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + "loginId:" + loginId);
        } catch (Exception e) {
            log.warn("Redis 삭제 실패: {}", e.getMessage());
        }
    }

    // 토큰 쌍 반환용 record
    public record TokenPair(String accessToken, String refreshToken) {}
}
