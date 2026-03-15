package com.notecurve.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.notecurve.auth.domain.RefreshToken;
import com.notecurve.auth.repository.RefreshTokenRepository;
import com.notecurve.auth.security.JwtTokenProvider;
import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    // ========== login 테스트 ==========

    @Test
    @DisplayName("로그인 성공 - 액세스/리프레시 토큰 반환 및 Redis 저장")
    void login_success() {
        // Given
        User mockUser = mock(User.class);
        when(mockUser.getPassword()).thenReturn("encodedPassword");

        when(userRepository.findByLoginId("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("testUser")).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken("testUser")).thenReturn("refreshToken");
        when(refreshTokenRepository.save(any())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        AuthService.TokenPair result = authService.login("testUser", "rawPassword");

        // Then
        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");
        verify(refreshTokenRepository, times(1)).save(any());
        verify(valueOperations, times(2)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("로그인 성공 - 재로그인 시 기존 Redis 토큰 삭제 후 새 토큰 저장")
    void login_success_relogin_deletesOldToken() {
        // Given
        User mockUser = mock(User.class);
        when(mockUser.getPassword()).thenReturn("encodedPassword");
        when(userRepository.findByLoginId("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("testUser")).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken("testUser")).thenReturn("newRefreshToken");
        when(refreshTokenRepository.save(any())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 기존 토큰이 있는 상황
        when(valueOperations.get("refresh_token:loginId:testUser")).thenReturn("oldRefreshToken");

        // When
        authService.login("testUser", "rawPassword");

        // Then - 기존 토큰 키 삭제됐는지 검증
        verify(redisTemplate, times(1)).delete("refresh_token:token:oldRefreshToken");
        verify(redisTemplate, times(1)).delete("refresh_token:loginId:testUser");
    }

    @Test
    @DisplayName("로그인 성공 - Redis 죽어도 DB에 저장됨")
    void login_success_redisDown() {
        // Given
        User mockUser = mock(User.class);
        when(mockUser.getPassword()).thenReturn("encodedPassword");

        when(userRepository.findByLoginId("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("testUser")).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken("testUser")).thenReturn("refreshToken");
        when(refreshTokenRepository.save(any())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis 연결 실패"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // When
        AuthService.TokenPair result = authService.login("testUser", "rawPassword");

        // Then
        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");
        verify(refreshTokenRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 아이디")
    void login_fail_userNotFound() {
        // Given
        when(userRepository.findByLoginId("wrongUser")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login("wrongUser", "anyPassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 틀림")
    void login_fail_wrongPassword() {
        // Given
        User mockUser = mock(User.class);
        when(mockUser.getPassword()).thenReturn("encodedPassword");

        when(userRepository.findByLoginId("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login("testUser", "wrongPassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 올바르지 않습니다.");
    }

    // ========== refresh 테스트 ==========

    @Test
    @DisplayName("토큰 재발급 성공 - Redis에서 토큰 조회")
    void refresh_success_fromRedis() {
        // Given
        when(jwtTokenProvider.validateRefreshToken("validRefreshToken")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:token:validRefreshToken")).thenReturn("testUser");
        when(jwtTokenProvider.generateAccessToken("testUser")).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken("testUser")).thenReturn("newRefreshToken");
        when(refreshTokenRepository.save(any())).thenReturn(null);

        // When
        AuthService.TokenPair result = authService.refresh("validRefreshToken");

        // Then
        assertThat(result.accessToken()).isEqualTo("newAccessToken");
        assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    @DisplayName("토큰 재발급 성공 - Redis 없을 때 DB에서 조회 (Fallback)")
    void refresh_success_fromDB_fallback() {
        // Given
        RefreshToken storedToken = new RefreshToken("validRefreshToken", "testUser",
                LocalDateTime.now().plusDays(1));

        when(jwtTokenProvider.validateRefreshToken("validRefreshToken")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:token:validRefreshToken")).thenReturn(null);
        when(refreshTokenRepository.findByToken("validRefreshToken")).thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.generateAccessToken("testUser")).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken("testUser")).thenReturn("newRefreshToken");
        when(refreshTokenRepository.save(any())).thenReturn(null);

        // When
        AuthService.TokenPair result = authService.refresh("validRefreshToken");

        // Then
        assertThat(result.accessToken()).isEqualTo("newAccessToken");
        assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
        verify(refreshTokenRepository, times(1)).findByToken("validRefreshToken");
    }

    @Test
    @DisplayName("토큰 재발급 성공 - Redis 죽어도 DB에서 조회")
    void refresh_success_redisDown() {
        // Given
        RefreshToken storedToken = new RefreshToken("validRefreshToken", "testUser",
                LocalDateTime.now().plusDays(1));

        when(jwtTokenProvider.validateRefreshToken("validRefreshToken")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis 연결 실패"));
        when(refreshTokenRepository.findByToken("validRefreshToken")).thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.generateAccessToken("testUser")).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken("testUser")).thenReturn("newRefreshToken");
        when(refreshTokenRepository.save(any())).thenReturn(null);

        // When
        AuthService.TokenPair result = authService.refresh("validRefreshToken");

        // Then
        assertThat(result.accessToken()).isEqualTo("newAccessToken");
        assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
        verify(refreshTokenRepository, times(1)).findByToken("validRefreshToken");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - JWT 자체가 유효하지 않음")
    void refresh_fail_invalidJwt() {
        // Given
        when(jwtTokenProvider.validateRefreshToken("invalidToken")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refresh("invalidToken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis, DB 모두 토큰 없음")
    void refresh_fail_tokenNotFound() {
        // Given
        when(jwtTokenProvider.validateRefreshToken("unknownToken")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:token:unknownToken")).thenReturn(null);
        when(refreshTokenRepository.findByToken("unknownToken")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refresh("unknownToken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - DB상 토큰 만료")
    void refresh_fail_expiredInDB() {
        // Given
        RefreshToken expiredToken = new RefreshToken("expiredToken", "testUser",
                LocalDateTime.now().minusDays(1));

        when(jwtTokenProvider.validateRefreshToken("expiredToken")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:token:expiredToken")).thenReturn(null);
        when(refreshTokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> authService.refresh("expiredToken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");

        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }

    // ========== logout 테스트 ==========

    @Test
    @DisplayName("로그아웃 성공 - DB + Redis에서 리프레시 토큰 삭제")
    void logout_success() {
        // Given
        doNothing().when(refreshTokenRepository).deleteByLoginId("testUser");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:loginId:testUser")).thenReturn(null);

        // When
        authService.logout("testUser");

        // Then
        verify(refreshTokenRepository, times(1)).deleteByLoginId("testUser");
        verify(redisTemplate, times(1)).delete("refresh_token:loginId:testUser");
    }

    @Test
    @DisplayName("로그아웃 성공 - Redis 죽어도 DB에서 삭제")
    void logout_success_redisDown() {
        // Given
        doNothing().when(refreshTokenRepository).deleteByLoginId("testUser");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis 연결 실패"));

        // When
        authService.logout("testUser");

        // Then
        verify(refreshTokenRepository, times(1)).deleteByLoginId("testUser");
    }
}
