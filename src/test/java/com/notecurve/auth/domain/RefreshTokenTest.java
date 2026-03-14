package com.notecurve.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    @DisplayName("만료되지 않은 토큰 - isExpired() false 반환")
    void isExpired_notExpired_returnsFalse() {
        // Given
        RefreshToken token = new RefreshToken("token-value", "testUser", LocalDateTime.now().plusDays(1));

        // When
        boolean result = token.isExpired();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 - isExpired() true 반환")
    void isExpired_expired_returnsTrue() {
        // Given
        RefreshToken token = new RefreshToken("token-value", "testUser", LocalDateTime.now().minusSeconds(1));

        // When
        boolean result = token.isExpired();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("생성자로 설정된 필드값 확인")
    void constructor_setsFieldsCorrectly() {
        // Given
        LocalDateTime expiry = LocalDateTime.now().plusDays(14);

        // When
        RefreshToken token = new RefreshToken("my-token", "testUser", expiry);

        // Then
        assertThat(token.getToken()).isEqualTo("my-token");
        assertThat(token.getLoginId()).isEqualTo("testUser");
        assertThat(token.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("정확히 현재 시각과 일치하는 만료일 - isExpired() true 반환")
    void isExpired_justExpired_returnsTrue() {
        // Given
        RefreshToken token = new RefreshToken("token-value", "testUser", LocalDateTime.now().minusNanos(1));

        // When
        boolean result = token.isExpired();

        // Then
        assertThat(result).isTrue();
    }
}
