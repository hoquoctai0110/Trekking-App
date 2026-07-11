package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDateTime;

public record AdminCheckInResponse(
        Long id,
        String user,
        String tour,
        String checkpoint,
        LocalDateTime checkInTime,
        String status,
        Double latitude,
        Double longitude
) {
}
