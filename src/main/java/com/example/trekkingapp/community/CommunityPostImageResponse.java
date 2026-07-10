package com.example.trekkingapp.community;

import java.time.LocalDateTime;

public record CommunityPostImageResponse(
        Long imageId,
        String imageUrl,
        Integer displayOrder,
        LocalDateTime createdAt
) {
}
