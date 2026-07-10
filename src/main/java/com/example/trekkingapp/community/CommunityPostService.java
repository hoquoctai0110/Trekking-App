package com.example.trekkingapp.community;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.media.MediaStorageService;
import com.example.trekkingapp.media.UploadResult;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommunityPostService {

    private static final Logger log = LoggerFactory.getLogger(CommunityPostService.class);

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    private static final String ROLE_ADMIN = "ADMIN";

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostImageRepository communityPostImageRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final MediaStorageService mediaStorageService;

    public CommunityPostService(
            CommunityPostRepository communityPostRepository,
            CommunityPostImageRepository communityPostImageRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            MediaStorageService mediaStorageService
    ) {
        this.communityPostRepository = communityPostRepository;
        this.communityPostImageRepository = communityPostImageRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public CommunityPostResponse createPost(CommunityPostRequest request) {
        User currentUser = findCurrentUser();

        CommunityPost post = new CommunityPost();
        post.setAuthor(currentUser);
        post.setContent(request.content());
        post.setStatus(STATUS_ACTIVE);

        return toResponse(communityPostRepository.save(post), true);
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> findPosts() {
        return communityPostRepository.findActivePosts(STATUS_DELETED)
                .stream()
                .map(post -> toResponse(post, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityPostResponse findPostById(Long postId) {
        return toResponse(findActivePost(postId), true);
    }

    @Transactional
    public String deletePost(Long postId) {
        CommunityPost post = findActivePost(postId);
        validateOwnership(post);

        List<CommunityPostImage> images = communityPostImageRepository.findByPost_PostIdOrderByDisplayOrderAscImageIdAsc(postId);
        List<String> publicIds = images.stream()
                .map(CommunityPostImage::getPublicId)
                .toList();
        communityPostImageRepository.deleteAll(images);
        communityPostImageRepository.flush();

        for (String publicId : publicIds) {
            mediaStorageService.deleteImage(publicId);
        }

        post.setStatus(STATUS_DELETED);
        communityPostRepository.save(post);
        return "Community post deleted successfully";
    }

    @Transactional
    public List<CommunityPostImageResponse> uploadPostImages(Long postId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        CommunityPost post = findActivePost(postId);
        validateOwnership(post);

        List<UploadResult> uploadedFiles = new ArrayList<>();
        try {
            List<CommunityPostImage> existingImages = communityPostImageRepository.findByPost_PostIdOrderByDisplayOrderAscImageIdAsc(postId);
            int nextDisplayOrder = existingImages.size();
            List<CommunityPostImageResponse> responses = new ArrayList<>();

            for (MultipartFile file : files) {
                UploadResult uploadResult = mediaStorageService.uploadImage(file, "community/posts/" + postId);
                uploadedFiles.add(uploadResult);

                CommunityPostImage image = new CommunityPostImage();
                image.setPost(post);
                image.setImageUrl(resolveImageUrl(uploadResult));
                image.setPublicId(uploadResult.publicId());
                image.setDisplayOrder(nextDisplayOrder++);
                responses.add(toImageResponse(communityPostImageRepository.save(image)));
            }

            return responses;
        } catch (RuntimeException exception) {
            cleanupUploadedResults(uploadedFiles);
            throw exception;
        }
    }

    private CommunityPost findActivePost(Long postId) {
        return communityPostRepository.findActivePostById(postId, STATUS_DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community post not found"));
    }

    private User findCurrentUser() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    private void validateOwnership(CommunityPost post) {
        User currentUser = findCurrentUser();
        if (post.getAuthor().getUserId().equals(currentUser.getUserId()) || hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this community post");
    }

    private boolean hasRole(User user, String roleName) {
        return user.getUserRoles()
                .stream()
                .anyMatch(userRole -> roleName.equals(userRole.getRole().getRoleName()));
    }

    private CommunityPostResponse toResponse(CommunityPost post, boolean includeImages) {
        User author = post.getAuthor();
        List<CommunityPostImageResponse> images = includeImages
                ? communityPostImageRepository.findByPost_PostIdOrderByDisplayOrderAscImageIdAsc(post.getPostId())
                .stream()
                .map(this::toImageResponse)
                .toList()
                : null;
        return new CommunityPostResponse(
                post.getPostId(),
                author.getUserId(),
                author.getFullName(),
                author.getAvatarUrl(),
                post.getContent(),
                post.getStatus(),
                images,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private CommunityPostImageResponse toImageResponse(CommunityPostImage image) {
        return new CommunityPostImageResponse(
                image.getImageId(),
                image.getImageUrl(),
                image.getDisplayOrder(),
                image.getCreatedAt()
        );
    }

    private String resolveImageUrl(UploadResult uploadResult) {
        return uploadResult.secureUrl() == null || uploadResult.secureUrl().isBlank()
                ? uploadResult.url()
                : uploadResult.secureUrl();
    }

    private void cleanupUploadedResults(List<UploadResult> uploadedFiles) {
        for (UploadResult uploadedFile : uploadedFiles) {
            if (uploadedFile == null || uploadedFile.publicId() == null || uploadedFile.publicId().isBlank()) {
                continue;
            }
            try {
                mediaStorageService.deleteImage(uploadedFile.publicId());
            } catch (RuntimeException cleanupException) {
                log.warn("community_image_cleanup_failed publicId={} message={}",
                        uploadedFile.publicId(),
                        cleanupException.getMessage());
            }
        }
    }
}
