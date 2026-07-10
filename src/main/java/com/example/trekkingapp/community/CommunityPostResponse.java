package com.example.trekkingapp.community;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostResponse(
        Long postId,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        String status,
        List<CommunityPostImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
