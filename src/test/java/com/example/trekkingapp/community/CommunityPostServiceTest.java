package com.example.trekkingapp.community;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.media.MediaStorageService;
import com.example.trekkingapp.media.UploadResult;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceTest {

    @Mock
    private CommunityPostRepository communityPostRepository;

    @Mock
    private CommunityPostImageRepository communityPostImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MediaStorageService mediaStorageService;

    private CommunityPostService communityPostService;

    @BeforeEach
    void setUp() {
        communityPostService = new CommunityPostService(
                communityPostRepository,
                communityPostImageRepository,
                userRepository,
                currentUserService,
                mediaStorageService
        );
    }

    @Test
    void uploadPostImagesStoresGalleryImages() {
        User author = new User();
        author.setUserId(500L);
        author.setFullName("Author");

        CommunityPost post = new CommunityPost();
        post.setPostId(12L);
        post.setAuthor(author);
        post.setContent("Test");
        post.setStatus("ACTIVE");

        MockMultipartFile file = new MockMultipartFile("files", "post.jpg", "image/jpeg", new byte[]{1});

        when(currentUserService.getCurrentUserId()).thenReturn(500L);
        when(userRepository.findById(500L)).thenReturn(Optional.of(author));
        when(communityPostRepository.findActivePostById(12L, "DELETED")).thenReturn(Optional.of(post));
        when(communityPostImageRepository.findByPost_PostIdOrderByDisplayOrderAscImageIdAsc(12L)).thenReturn(List.of());
        when(mediaStorageService.uploadImage(file, "community/posts/12"))
                .thenReturn(new UploadResult(null, "https://cdn.example.com/community/12/post.jpg", "post-public", 100, 100, "jpg", 1000L));
        when(communityPostImageRepository.save(any(CommunityPostImage.class))).thenAnswer(invocation -> {
            CommunityPostImage image = invocation.getArgument(0);
            image.setImageId(1L);
            return image;
        });

        List<CommunityPostImageResponse> response = communityPostService.uploadPostImages(12L, List.of(file));

        assertEquals(1, response.size());
        assertEquals("https://cdn.example.com/community/12/post.jpg", response.getFirst().imageUrl());
        assertEquals(0, response.getFirst().displayOrder());
    }
}
