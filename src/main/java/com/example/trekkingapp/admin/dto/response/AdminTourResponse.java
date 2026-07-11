package com.example.trekkingapp.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTourResponse(
        Long id,
        String name,
        String description,
        String coverImage,
        String difficulty,
        Double distance,
        String duration,
        Double elevation,
        String province,
        String tourProvider,
        Integer maxParticipants,
        BigDecimal price,
        String status,
        Double rating,
        long reviewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
