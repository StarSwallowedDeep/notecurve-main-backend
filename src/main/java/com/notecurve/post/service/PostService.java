package com.notecurve.post.service;

import com.notecurve.post.domain.Post;
import com.notecurve.post.domain.PostImage;
import com.notecurve.post.dto.PostRequestDto;
import com.notecurve.post.dto.PostResponseDto;
import com.notecurve.post.repository.PostRepository;
import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;
import com.notecurve.image.service.ImageUploadService;
import com.notecurve.kafka.producer.EventProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private static final Pattern IMAGE_PATTERN = Pattern.compile("(?i).*\\.(jpg|jpeg|png|gif)$");
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageUploadService imageUploadService;
    private final StringRedisTemplate redisTemplate;
    private final EventProducer eventProducer;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ========================= 게시글 관련 =========================

    @Transactional
    @CacheEvict(value = "posts", allEntries = true) // 게시글 저장 시 전체 목록 캐시 삭제
    public PostResponseDto savePost(PostRequestDto postRequestDto, Long userId, String idempotencyKey) throws IOException {
        // 중복 요청 체크
        if (idempotencyKey != null) {
            try {
                if (Boolean.TRUE.equals(redisTemplate.hasKey(IDEMPOTENCY_PREFIX + idempotencyKey))) {
                    throw new IllegalArgumentException("중복 요청입니다.");
                }
                redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + idempotencyKey, "1", 5, TimeUnit.MINUTES);
            } catch (IllegalArgumentException e) {
                throw e; // 중복 요청은 그대로 던짐
            } catch (Exception e) {
                log.warn("Redis 장애로 멱등성 체크 생략: {}", e.getMessage());
                // Redis 죽으면 그냥 통과
            }
        }

        try {
            User user = getUserOrThrow(userId);

            String thumbnailUrl = postRequestDto.getThumbnail();
            if (thumbnailUrl != null && !thumbnailUrl.isBlank()) validateImageExtension(thumbnailUrl);

            Post post = Post.builder()
                    .title(postRequestDto.getTitle())
                    .subtitle(postRequestDto.getSubtitle())
                    .category(postRequestDto.getCategory())
                    .content(postRequestDto.getContent())
                    .thumbnailImageUrl(thumbnailUrl)
                    .date(LocalDate.now())
                    .user(user)
                    .build();

            List<PostImage> postImages = saveContentImages(postRequestDto.getContentImages(), post);
            post.setContentImageUrls(postImages);

            Post savedPost = postRepository.save(post);
            eventProducer.sendPostEvent(
                "CREATED", 
                savedPost.getId(),
                user.getId(),
                savedPost.getTitle(),
                savedPost.getUser().getName(), 
                savedPost.getDate()
            );
            return buildPostResponseDto(savedPost);

        } catch (Exception e) {
            // 실패 시 idempotency 키 삭제
            if (idempotencyKey != null) {
                try {
                    redisTemplate.delete(IDEMPOTENCY_PREFIX + idempotencyKey);
                } catch (Exception redisEx) {
                    log.warn("Redis 장애로 멱등성 키 삭제 생략: {}", redisEx.getMessage());
                }
            }
            if (postRequestDto.getThumbnail() != null) {
                imageUploadService.deleteFile(postRequestDto.getThumbnail());
            }
            if (postRequestDto.getContentImages() != null) {
                postRequestDto.getContentImages().forEach(imageUploadService::deleteFile);
            }
            throw e;
        }
    }

    @Cacheable(value = "post", key = "#postId") // 단건 조회 캐시
    public PostResponseDto getPost(Long postId) {
        Post post = getPostOrThrow(postId);
        return buildPostResponseDto(post);
    }

    @Cacheable(value = "posts") // 전체 목록 캐시
    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAllWithUser().stream()
                .map(this::buildPostResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "post", key = "#postId"), // 단일 캐시 삭제
        @CacheEvict(value = "posts", allEntries = true) // 목록 캐시 삭제
    })
    public PostResponseDto updatePost(Long postId, PostRequestDto postRequestDto, Long userId) throws IOException {
        Post post = getPostOrThrow(postId);
        User loginUser = getUserOrThrow(userId);

        verifyOwnership(post, loginUser);

        // ========================= 본문 이미지 처리 (Set 기반) =========================
        List<PostImage> contentImages = post.getContentImageUrls();
        Set<String> existingImageUrls = contentImages.stream()
                .map(PostImage::getContentImageUrl)
                .collect(Collectors.toSet());

        Set<String> newImageUrls = new HashSet<>(Optional.ofNullable(postRequestDto.getContentImages())
                .orElse(Collections.emptyList()));

        // 삭제할 이미지
        List<PostImage> imagesToDelete = contentImages.stream()
                .filter(img -> !newImageUrls.contains(img.getContentImageUrl()))
                .toList();

        // DB에서 제거
        contentImages.removeAll(imagesToDelete);

        // 새로 추가할 이미지
        Set<String> imagesToAddUrls = new HashSet<>(newImageUrls);
        imagesToAddUrls.removeAll(existingImageUrls);
        List<PostImage> addedImages = saveContentImages(new ArrayList<>(imagesToAddUrls), post);

        // 최종 이미지 리스트 구성
        List<PostImage> finalImages = new ArrayList<>(contentImages);
        finalImages.addAll(addedImages);

        // ========================= 썸네일 변경 처리 =========================
        String oldThumbnail = post.getThumbnailImageUrl();
        String newThumbnail = postRequestDto.getThumbnail();

        if (newThumbnail != null && !newThumbnail.equals(oldThumbnail)) {

            // 확장자 체크
            validateImageExtension(newThumbnail);

            // 커밋 후 기존 파일 삭제
            if (oldThumbnail != null && !oldThumbnail.isBlank()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deleteFile(oldThumbnail);
                    }
                });
            }
        }

        // ========================= 텍스트 및 이미지 반영 =========================
        post.updateFrom(postRequestDto, finalImages);

        Post updatedPost = postRepository.save(post);

        // ========================= 본문 이미지 파일 삭제 (커밋 이후) =========================
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imagesToDelete.forEach(img -> deleteFile(img.getContentImageUrl()));
            }
        });

        return buildPostResponseDto(updatedPost);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "post", key = "#postId"), // 단일 캐시 삭제
        @CacheEvict(value = "posts", allEntries = true) // 목록 캐시 삭제
    })
    public void deletePost(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        User loginUser = getUserOrThrow(userId);

        verifyOwnership(post, loginUser);

        // 삭제할 파일 URL 수집
        List<String> filesToDelete = new ArrayList<>();
        if (post.getThumbnailImageUrl() != null && !post.getThumbnailImageUrl().isBlank()) {
            filesToDelete.add(post.getThumbnailImageUrl());
        }
        Optional.ofNullable(post.getContentImageUrls())
                .orElse(Collections.emptyList())
                .forEach(img -> filesToDelete.add(img.getContentImageUrl()));

        // DB 삭제
        postRepository.delete(post);
        eventProducer.sendPostEvent("DELETED", postId, null, null, null, null);

        // 트랜잭션 커밋 후 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                filesToDelete.forEach(PostService.this::deleteFile);
            }
        });
    }

    // ========================= 이미지 관련 =========================

    private PostImage saveImage(String imageUrl, Post post) throws IOException {
        validateImageExtension(imageUrl);
        return PostImage.builder()
            .contentImageUrl(imageUrl)
            .post(post)
            .build();
    }

    private List<PostImage> saveContentImages(List<String> imageUrls, Post post) throws IOException {
        if (imageUrls == null || imageUrls.isEmpty()) return Collections.emptyList();

        List<PostImage> images = new ArrayList<>();
        for (String url : imageUrls) {
            images.add(saveImage(url, post));
        }
        return images;
    }

    private void validateImageExtension(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        if (!IMAGE_PATTERN.matcher(fileName).matches()) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + fileName);
        }
    }

    private void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        Path filePath = Paths.get(uploadDir, fileName);

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath);
            } else {
                log.warn("File not found: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Error while deleting file: {}", filePath, e);
        }
    }

    // ========================= 관리자 관련 =========================

    // 게시글 수 카운트
    public long countPosts() {
        return postRepository.count();
    }

    // 강제 삭제 (관리자용 - 소유자 체크 없음)
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "post", key = "#postId"),
        @CacheEvict(value = "posts", allEntries = true)
    })
    public void adminDeletePost(Long postId) {
        Post post = getPostOrThrow(postId);

        List<String> filesToDelete = new ArrayList<>();
        if (post.getThumbnailImageUrl() != null) {
            filesToDelete.add(post.getThumbnailImageUrl());
        }
        Optional.ofNullable(post.getContentImageUrls())
                .orElse(Collections.emptyList())
                .forEach(img -> filesToDelete.add(img.getContentImageUrl()));

        postRepository.delete(post);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                filesToDelete.forEach(PostService.this::deleteFile);
            }
        });
    }

    // ========================= DTO 관련 =========================

    private PostResponseDto buildPostResponseDto(Post post) {
        String userName = post.getUser() != null && post.getUser().getName() != null
                ? post.getUser().getName()
                : "알 수 없음";

        List<String> contentImageUrls = Optional.ofNullable(post.getContentImageUrls())
                .orElse(Collections.emptyList())
                .stream()
                .map(PostImage::getContentImageUrl)
                .toList();

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .subtitle(post.getSubtitle())
                .category(post.getCategory())
                .content(post.getContent())
                .thumbnailImageUrl(post.getThumbnailImageUrl())
                .contentImageUrls(contentImageUrls)
                .date(post.getDate())
                .userName(userName)
                .userId(post.getUser() != null ? post.getUser().getId() : null)
                .profileImage(post.getUser() != null ? post.getUser().getProfileImage() : null)
                .build();
    }

    // ========================= 헬퍼 메서드 =========================

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findByIdWithUser(postId) 
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
    }

    private void verifyOwnership(Post post, User user) {
        if (!post.getUser().getId().equals(user.getId())) {
            throw new SecurityException("작성자만 수행할 수 있습니다.");
        }
    }
}
