package com.notecurve.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 테스트용 시크릿 키 (32자 이상이어야 HS256에서 동작)
    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-must-be-long-enough";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // @Value로 주입되는 값을 테스트에서 직접 세팅
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", TEST_SECRET);
        // @PostConstruct 대신 직접 init() 호출
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("액세스 토큰 생성 - loginId가 토큰에 담겨야 한다")
    void generateAccessToken_success() {
        // Given
        String loginId = "testUser";

        // When
        String token = jwtTokenProvider.generateAccessToken(loginId);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.getLoginIdFromToken(token)).isEqualTo(loginId);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 - loginId가 토큰에 담겨야 한다")
    void generateRefreshToken_success() {
        // Given
        String loginId = "testUser";

        // When
        String token = jwtTokenProvider.generateRefreshToken(loginId);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.getLoginIdFromToken(token)).isEqualTo(loginId);
    }

    @Test
    @DisplayName("유효한 토큰 검증 - true 반환")
    void validateToken_validToken_returnsTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken("testUser");

        // When
        boolean result = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 검증 - false 반환")
    void validateToken_invalidToken_returnsFalse() {
        // Given
        String invalidToken = "this.is.not.a.valid.token";

        // When
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 - false 반환")
    void validateToken_expiredToken_returnsFalse() throws Exception {
        // Given: 만료 시간을 -1ms로 설정해서 이미 만료된 토큰 생성
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "secretKey", TEST_SECRET);
        expiredProvider.init();

        // generateToken은 private이므로 리플렉션으로 직접 호출
        java.lang.reflect.Method method = JwtTokenProvider.class
                .getDeclaredMethod("generateToken", String.class, long.class);
        method.setAccessible(true);
        String expiredToken = (String) method.invoke(expiredProvider, "testUser", -1000L);

        // When
        boolean result = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰에서 loginId 추출 - 정확히 반환")
    void getLoginIdFromToken_returnsCorrectLoginId() {
        // Given
        String loginId = "hello@example.com";
        String token = jwtTokenProvider.generateAccessToken(loginId);

        // When
        String extracted = jwtTokenProvider.getLoginIdFromToken(token);

        // Then
        assertThat(extracted).isEqualTo(loginId);
    }
}
