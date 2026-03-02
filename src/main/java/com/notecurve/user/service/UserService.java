package com.notecurve.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;
import com.notecurve.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    // 회원가입 메서드
    @Transactional
    public void register(User user) {
        if (userRepository.existsByLoginId(user.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if (!user.getName().matches("^[a-zA-Z0-9가-힣]{2,10}$")) {
            throw new IllegalArgumentException("이름은 2~10자의 영어, 한글, 숫자만 가능합니다.");
        }

        if (userRepository.existsByName(user.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }

        if (!user.getLoginId().matches("^[a-zA-Z0-9]{7,20}$")) {
            throw new IllegalArgumentException("아이디는 영문자와 숫자 조합으로 7~20자여야 합니다.");
        }

        if (!user.getPassword().matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,20}$")) {
            throw new IllegalArgumentException("비밀번호는 영문자, 숫자, 특수문자를 모두 포함하고 8자 이상이어야 합니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    // 패스워드 변경 메서드
    @Transactional
    public void updatePassword(Long userId, String loginId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!user.getLoginId().equals(loginId)) {
            throw new IllegalStateException("본인 계정만 변경할 수 있습니다.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("이전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
        }

        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$")) {
            throw new IllegalArgumentException("비밀번호는 영문자, 숫자, 특수문자를 모두 포함하고 8자 이상이어야 합니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    // 이름 변경 메서드
    @Transactional
    public void updateName(Long userId, String loginId, String newName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!user.getLoginId().equals(loginId)) {
            throw new IllegalStateException("본인 계정만 변경할 수 있습니다.");
        }

        if (!newName.matches("^[a-zA-Z0-9가-힣]{2,10}$")) {
            throw new IllegalArgumentException("이름은 2~10자의 영어, 한글, 숫자만 가능합니다.");
        }

        if (userRepository.existsByName(newName)) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }

        user.setName(newName);
        userRepository.save(user);
    }

    // 회원탈퇴 메서드
    @Transactional
    public void deleteUser(Long userId, String loginId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!user.getLoginId().equals(loginId)) {
            throw new IllegalStateException("본인 계정만 탈퇴할 수 있습니다.");
        }

        refreshTokenRepository.deleteByLoginId(loginId);
        userRepository.delete(user);
    }

    // 로그인 ID로 사용자 찾기
    public User findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
