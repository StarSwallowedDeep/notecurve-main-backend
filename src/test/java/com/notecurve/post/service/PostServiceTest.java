package com.notecurve.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.notecurve.image.service.ImageUploadService;
import com.notecurve.post.domain.Post;
import com.notecurve.post.domain.PostImage;
import com.notecurve.post.dto.PostRequestDto;
import com.notecurve.post.dto.PostResponseDto;
import com.notecurve.post.repository.PostRepository;
import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "uploadDir", "/tmp/test-uploads");
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
    }

    // ========== savePost 테스트 ==========

    @Test
    @DisplayName("게시글 저장 성공 - 모든 허용 확장자 저장 가능")
    void savePost_allowedExtensions_success() throws IOException {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);
        when(testUser.getName()).thenReturn("testUser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif");

        for (String ext : allowedExtensions) {
            Post savedPost = mock(Post.class);
            when(savedPost.getTitle()).thenReturn("testTitle");
            when(savedPost.getUser()).thenReturn(testUser);
            when(savedPost.getContentImageUrls()).thenReturn(new ArrayList<>());
            when(postRepository.save(any())).thenReturn(savedPost);

            PostRequestDto testRequest = PostRequestDto.builder()
                    .title("testTitle")
                    .subtitle("testSubtitle")
                    .category("testCategory")
                    .content("testContent")
                    .thumbnail("/images/testImage." + ext)
                    .build();

            // When
            PostResponseDto result = postService.savePost(testRequest, 1L);

            // Then
            assertThat(result.getTitle()).isEqualTo("testTitle");
        }
    }

    @Test
    @DisplayName("게시글 저장 실패 - 존재하지 않는 사용자")
    void savePost_fail_userNotFound() {
        // Given
        PostRequestDto testRequest = PostRequestDto.builder()
                .title("testTitle")
                .subtitle("testSubtitle")
                .category("testCategory")
                .content("testContent")
                .thumbnail("/images/testImage.jpg")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.savePost(testRequest, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("게시글 저장 실패 - 허용되지 않는 썸네일 확장자")
    void savePost_fail_invalidThumbnailExtension() {
        // Given
        User testUser = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        PostRequestDto testRequest = PostRequestDto.builder()
                .title("testTitle")
                .subtitle("testSubtitle")
                .category("testCategory")
                .content("testContent")
                .thumbnail("/images/testFile.exe")
                .build();

        // When & Then
        assertThatThrownBy(() -> postService.savePost(testRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않는 파일 형식입니다: testFile.exe");
    }

    // ========== getPost 테스트 ==========

    @Test
    @DisplayName("게시글 단건 조회 성공 - PostResponseDto 반환")
    void getPost_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getName()).thenReturn("testUser");

        Post testPost = mock(Post.class);
        when(testPost.getTitle()).thenReturn("testTitle");
        when(testPost.getUser()).thenReturn(testUser);
        when(testPost.getContentImageUrls()).thenReturn(new ArrayList<>());

        when(postRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testPost));

        // When
        PostResponseDto result = postService.getPost(1L);

        // Then
        assertThat(result.getTitle()).isEqualTo("testTitle");
    }

    @Test
    @DisplayName("게시글 단건 조회 실패 - 존재하지 않는 게시글")
    void getPost_fail_postNotFound() {
        // Given
        when(postRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.getPost(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시물을 찾을 수 없습니다.");
    }

    // ========== getAllPosts 테스트 ==========

    @Test
    @DisplayName("게시글 전체 조회 성공 - PostResponseDto 리스트 반환")
    void getAllPosts_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getName()).thenReturn("testUser");

        Post testPost1 = mock(Post.class);
        when(testPost1.getTitle()).thenReturn("testTitle1");
        when(testPost1.getUser()).thenReturn(testUser);
        when(testPost1.getContentImageUrls()).thenReturn(new ArrayList<>());

        Post testPost2 = mock(Post.class);
        when(testPost2.getTitle()).thenReturn("testTitle2");
        when(testPost2.getUser()).thenReturn(testUser);
        when(testPost2.getContentImageUrls()).thenReturn(new ArrayList<>());

        when(postRepository.findAllWithUser()).thenReturn(List.of(testPost1, testPost2));

        // When
        List<PostResponseDto> result = postService.getAllPosts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("testTitle1");
        assertThat(result.get(1).getTitle()).isEqualTo("testTitle2");
    }

    @Test
    @DisplayName("게시글 전체 조회 - 게시글 없으면 빈 리스트 반환")
    void getAllPosts_empty() {
        // Given
        when(postRepository.findAllWithUser()).thenReturn(new ArrayList<>());

        // When
        List<PostResponseDto> result = postService.getAllPosts();

        // Then
        assertThat(result).isEmpty();
    }

    // ========== deletePost 테스트 ==========

    @Test
    @DisplayName("게시글 삭제 성공 - DB에서 게시글 삭제")
    void deletePost_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Post testPost = mock(Post.class);
        when(testPost.getUser()).thenReturn(testUser);
        when(testPost.getThumbnailImageUrl()).thenReturn(null);
        when(testPost.getContentImageUrls()).thenReturn(new ArrayList<>());

        when(postRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testPost));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        postService.deletePost(1L, 1L);

        // Then
        verify(postRepository, times(1)).delete(testPost);
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자가 아니면 예외 발생")
    void deletePost_fail_notOwner() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(2L);

        Post testPost = mock(Post.class);
        when(testPost.getUser()).thenReturn(testUser);

        when(postRepository.findByIdWithUser(1L)).thenReturn(Optional.of(testPost));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        // When & Then
        assertThatThrownBy(() -> postService.deletePost(1L, 2L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("작성자만 수행할 수 있습니다.");
    }
}
