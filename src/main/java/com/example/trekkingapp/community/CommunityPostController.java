package com.example.trekkingapp.community;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community/posts")
public class CommunityPostController {

    private final CommunityPostService communityPostService;

    public CommunityPostController(CommunityPostService communityPostService) {
        this.communityPostService = communityPostService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommunityPostResponse> createPost(@Valid @RequestBody CommunityPostRequest request) {
        return new ApiResponse<>(true, "Community post created successfully", communityPostService.createPost(request));
    }

    @GetMapping
    public ApiResponse<List<CommunityPostResponse>> getPosts() {
        return new ApiResponse<>(true, "Community posts retrieved successfully", communityPostService.findPosts());
    }

    @GetMapping("/{postId}")
    public ApiResponse<CommunityPostResponse> getPost(@PathVariable Long postId) {
        return new ApiResponse<>(true, "Community post retrieved successfully", communityPostService.findPostById(postId));
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> deletePost(@PathVariable Long postId) {
        return new ApiResponse<>(true, "Community post deleted successfully", communityPostService.deletePost(postId));
    }

    @PostMapping("/{postId}/images")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CommunityPostImageResponse>> uploadPostImages(
            @PathVariable Long postId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return new ApiResponse<>(true, "Community post images uploaded successfully", communityPostService.uploadPostImages(postId, files));
    }
}
