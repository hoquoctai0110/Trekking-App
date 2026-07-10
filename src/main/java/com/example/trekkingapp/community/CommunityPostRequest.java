package com.example.trekkingapp.community;

import jakarta.validation.constraints.NotBlank;

public record CommunityPostRequest(
        @NotBlank(message = "content is required")
        String content
) {
}
