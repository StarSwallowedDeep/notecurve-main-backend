package com.notecurve.itletter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApiKeyInterceptorTest {

    private ApiKeyInterceptor apiKeyInterceptor;

    private static final String TEST_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        apiKeyInterceptor = new ApiKeyInterceptor();
        ReflectionTestUtils.setField(apiKeyInterceptor, "apiKey", TEST_API_KEY);
    }

    @Test
    @DisplayName("GET 요청은 API 키 없어도 통과")
    void preHandle_getRequest_returnsTrue() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("POST 요청 - 올바른 API 키면 통과")
    void preHandle_postRequest_validApiKey_returnsTrue() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-API-KEY", TEST_API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("POST 요청 - 잘못된 API 키면 401 반환")
    void preHandle_postRequest_invalidApiKey_returnsFalse() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-API-KEY", "wrong-api-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("POST 요청 - API 키 없으면 401 반환")
    void preHandle_postRequest_noApiKey_returnsFalse() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("DELETE 요청 - 올바른 API 키면 통과")
    void preHandle_deleteRequest_validApiKey_returnsTrue() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("DELETE");
        request.addHeader("X-API-KEY", TEST_API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        boolean result = apiKeyInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }
}
