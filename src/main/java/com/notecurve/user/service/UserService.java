package com.notecurve.user.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;
import com.notecurve.auth.repository.RefreshTokenRepository;
import com.notecurve.note.domain.Note;
import com.notecurve.note.repository.NoteRepository;
import com.notecurve.notefile.domain.NoteFile;
import com.notecurve.notefile.service.NoteFileService;
import com.notecurve.category.domain.Category;
import com.notecurve.category.repository.CategoryRepository;
import com.notecurve.post.domain.Post;
import com.notecurve.post.repository.PostRepository;
import com.notecurve.messageboard.repository.CommentRepository;
import com.notecurve.messageboard.repository.MessageBoardRepository;
import com.notecurve.image.service.ImageUploadService;
import com.notecurve.kafka.producer.EventProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    private final NoteRepository noteRepository;
    private final NoteFileService noteFileService;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MessageBoardRepository messageBoardRepository;
    private final ImageUploadService imageUploadService;

    private final EventProducer eventProducer;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @Value("${profile.upload-dir}")
    private String profileUploadDir;

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
        eventProducer.sendUserEvent(
            "CREATED", 
            user.getId(), 
            user.getLoginId(), 
            user.getName(), 
            user.getRole().name()
        );
    }

   // 프로필 이미지 저장 메서드
    @Transactional
    public void updateProfileImage(Long userId, String loginId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!user.getLoginId().equals(loginId)) {
            throw new IllegalStateException("본인 계정만 변경할 수 있습니다.");
        }

        // 기존 프로필 이미지 삭제
        if (user.getProfileImage() != null) {
            Path oldFile = Paths.get(profileUploadDir).resolve(user.getProfileImage());
            Files.deleteIfExists(oldFile);
        }

        // 새 이미지 저장
        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();
        String filename = "profile_" + userId + ext;
        Path savePath = Paths.get(profileUploadDir).resolve(filename);
        Files.createDirectories(savePath.getParent());
        Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

        user.setProfileImage(filename);
        userRepository.save(user);
    }

    // 프로필 이미지 삭제 메서드
    @Transactional
    public void deleteProfileImage(Long userId, String loginId) throws IOException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!user.getLoginId().equals(loginId)) {
            throw new IllegalStateException("본인 계정만 변경할 수 있습니다.");
        }

        if (user.getProfileImage() != null) {
            Path profileFile = Paths.get(profileUploadDir).resolve(user.getProfileImage());
            Files.deleteIfExists(profileFile);
            user.setProfileImage(null);
            userRepository.save(user);
        }
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

        eventProducer.sendUserEvent(
            "UPDATED", 
            user.getId(), 
            user.getLoginId(), 
            user.getName(), 
            user.getRole().name()
        );
    }

    // 회원탈퇴 메서드
    @Transactional
    public void deleteUser(Long userId, String loginId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!user.getLoginId().equals(loginId)) {
            throw new IllegalStateException("본인 계정만 탈퇴할 수 있습니다.");
        }

        if (user.getProfileImage() != null) {
            Path profileFile = Paths.get(profileUploadDir).resolve(user.getProfileImage());
            try {
                Files.deleteIfExists(profileFile);
            } catch (IOException e) {
                System.err.println("프로필 이미지 삭제 실패: " + profileFile);
            }
        }

        refreshTokenRepository.deleteByLoginId(loginId);
        deleteFromRedis(loginId);

        userRepository.delete(user);
        eventProducer.sendUserEvent("DELETED", userId, null, null, null);
    }

    // 전체 유저 목록 (관리자용)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 유저 수 카운트
    public long countUsers() {
        return userRepository.count();
    }

    // Role 변경 (관리자용)
    @Transactional
    public void updateUserRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        user.setRole(role);
        userRepository.save(user);
    }

    // 강제 탈퇴 (관리자용)
    @Transactional
    public void adminDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 1. NoteFile 물리적 파일 + DB 삭제
        List<Note> notes = noteRepository.findByUserWithFiles(user);
        for (Note note : notes) {
            if (note.getFiles() != null) {
                for (NoteFile file : note.getFiles()) {
                    try {
                        noteFileService.deletePhysicalFile(file);
                    } catch (IOException e) {
                        log.warn("NoteFile 물리적 삭제 실패: {}", file.getId());
                    }
                }
            }
        }

        // 2. Note 삭제 (NoteFile cascade로 같이 삭제)
        noteRepository.deleteAll(notes);

        // 3. Category 삭제
        List<Category> categories = categoryRepository.findByUser(user);
        categoryRepository.deleteAll(categories);

        // 4. Post 삭제 (PostImage cascade로 같이 삭제)
        List<Post> posts = postRepository.findByUser(user);
        for (Post post : posts) {
            // 썸네일 물리적 파일 삭제
            if (post.getThumbnailImageUrl() != null) {
                imageUploadService.deleteFile(post.getThumbnailImageUrl());
            }
            // 본문 이미지 물리적 파일 삭제
            if (post.getContentImageUrls() != null) {
                post.getContentImageUrls()
                    .forEach(img -> imageUploadService.deleteFile(img.getContentImageUrl()));
            }
        }
        postRepository.deleteAll(posts);

        // 5. Comment 삭제
        commentRepository.deleteByUser(user);

        // 6. MessageBoard 삭제 (Comment cascade로 같이 삭제)
        messageBoardRepository.deleteByUser(user);

        // 7. 프로필 이미지 물리적 파일 삭제
        if (user.getProfileImage() != null) {
            try {
                Path profileFile = Paths.get(profileUploadDir).resolve(user.getProfileImage());
                Files.deleteIfExists(profileFile);
            } catch (IOException e) {
                log.warn("프로필 이미지 삭제 실패: {}", user.getProfileImage());
            }
        }

        // 8. RefreshToken + Redis 삭제
        refreshTokenRepository.deleteByLoginId(user.getLoginId());
        deleteFromRedis(user.getLoginId());

        // 9. User 삭제
        userRepository.delete(user);
        eventProducer.sendUserEvent("DELETED", userId, null, null, null);
    }

    // 로그인 ID로 사용자 찾기
    public User findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    // ========== Redis 헬퍼 메서드 ==========

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
}
