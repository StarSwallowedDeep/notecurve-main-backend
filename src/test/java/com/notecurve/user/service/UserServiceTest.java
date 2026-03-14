package com.notecurve.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.notecurve.auth.repository.RefreshTokenRepository;
import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "profileUploadDir", tempDir.toString());
    }

    // ========== register 테스트 ==========

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getName()).thenReturn("testUser");
        when(testUser.getPassword()).thenReturn("testPass1!");

        when(userRepository.existsByLoginId("testUser1")).thenReturn(false);
        when(userRepository.existsByName("testUser")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        // When
        userService.register(testUser);

        // Then
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void register_fail_duplicateLoginId() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.existsByLoginId("testUser1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이름")
    void register_fail_duplicateName() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getName()).thenReturn("testUser");

        when(userRepository.existsByLoginId("testUser1")).thenReturn(false);
        when(userRepository.existsByName("testUser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이름입니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 7자 미만")
    void register_fail_loginIdTooShort() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("abc");
        when(testUser.getName()).thenReturn("testUser");

        when(userRepository.existsByLoginId("abc")).thenReturn(false);
        when(userRepository.existsByName("testUser")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디는 영문자와 숫자 조합으로 7~20자여야 합니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 한글 포함")
    void register_fail_loginIdInvalidChar() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("테스트아이디");
        when(testUser.getName()).thenReturn("testUser");

        when(userRepository.existsByLoginId("테스트아이디")).thenReturn(false);
        when(userRepository.existsByName("testUser")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디는 영문자와 숫자 조합으로 7~20자여야 합니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 특수문자 누락")
    void register_fail_passwordNoSpecialChar() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getName()).thenReturn("testUser");
        when(testUser.getPassword()).thenReturn("testPass1");

        when(userRepository.existsByLoginId("testUser1")).thenReturn(false);
        when(userRepository.existsByName("testUser")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 영문자, 숫자, 특수문자를 모두 포함하고 8자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 형식 오류")
    void register_fail_invalidPassword() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getName()).thenReturn("testUser");
        when(testUser.getPassword()).thenReturn("weakpassword");

        when(userRepository.existsByLoginId("testUser1")).thenReturn(false);
        when(userRepository.existsByName("testUser")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.register(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 영문자, 숫자, 특수문자를 모두 포함하고 8자 이상이어야 합니다.");
    }

    // ========== updatePassword 테스트 ==========

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getPassword()).thenReturn("encodedOldPass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("newPass1!", "encodedOldPass")).thenReturn(false);
        when(passwordEncoder.encode("newPass1!")).thenReturn("encodedNewPass");

        // When
        userService.updatePassword(1L, "testUser1", "newPass1!");

        // Then
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 존재하지 않는 사용자")
    void updatePassword_fail_userNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updatePassword(999L, "testUser1", "newPass1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 이전 비밀번호와 동일")
    void updatePassword_fail_samePassword() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getPassword()).thenReturn("encodedOldPass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("testPass1!", "encodedOldPass")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updatePassword(1L, "testUser1", "testPass1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 본인 계정이 아님")
    void updatePassword_fail_notOwner() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updatePassword(1L, "otherUser", "newPass1!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("본인 계정만 변경할 수 있습니다.");
    }

    // ========== updateName 테스트 ==========

    @Test
    @DisplayName("이름 변경 성공")
    void updateName_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByName("newName")).thenReturn(false);

        // When
        userService.updateName(1L, "testUser1", "newName");

        // Then
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("이름 변경 실패 - 존재하지 않는 사용자")
    void updateName_fail_userNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateName(999L, "testUser1", "newName"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");
    }

    @Test
    @DisplayName("이름 변경 실패 - 이름 1자")
    void updateName_fail_nameTooShort() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateName(1L, "testUser1", "a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 2~10자의 영어, 한글, 숫자만 가능합니다.");
    }

    @Test
    @DisplayName("이름 변경 실패 - 이름 10자 초과")
    void updateName_fail_nameTooLong() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateName(1L, "testUser1", "이름이열자를초과합니다"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 2~10자의 영어, 한글, 숫자만 가능합니다.");
    }

    @Test
    @DisplayName("이름 변경 실패 - 중복 이름")
    void updateName_fail_duplicateName() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByName("newName")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateName(1L, "testUser1", "newName"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이름입니다.");
    }

    @Test
    @DisplayName("이름 변경 실패 - 본인 계정이 아님")
    void updateName_fail_notOwner() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateName(1L, "otherUser", "newName"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("본인 계정만 변경할 수 있습니다.");
    }

    // ========== deleteUser 테스트 ==========

    @Test
    @DisplayName("회원 탈퇴 성공 - DB에서 사용자 삭제")
    void deleteUser_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getProfileImage()).thenReturn(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(1L, "testUser1");

        // Then
        verify(refreshTokenRepository, times(1)).deleteByLoginId("testUser1");
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 프로필 이미지도 함께 삭제")
    void deleteUser_success_withProfileImage() throws IOException {
        // Given
        Path testProfileImage = tempDir.resolve("testProfile.jpg");
        Files.createFile(testProfileImage);

        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");
        when(testUser.getProfileImage()).thenReturn("testProfile.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(1L, "testUser1");

        // Then
        verify(refreshTokenRepository, times(1)).deleteByLoginId("testUser1");
        verify(userRepository, times(1)).delete(testUser);
        assertThat(Files.exists(testProfileImage)).isFalse();
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 존재하지 않는 사용자")
    void deleteUser_fail_userNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(999L, "testUser1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 본인 계정이 아님")
    void deleteUser_fail_notOwner() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getLoginId()).thenReturn("testUser1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(1L, "otherUser"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("본인 계정만 탈퇴할 수 있습니다.");
    }
}
