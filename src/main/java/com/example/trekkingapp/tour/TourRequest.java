package com.example.trekkingapp.tour;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TourRequest(
        @NotNull(message = "routeId is required")
        Long routeId,

        @NotBlank(message = "title is required")
        String title,

        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "price must be greater than or equal to 0")
        BigDecimal price,

        @NotNull(message = "maxParticipants is required")
        @Min(value = 1, message = "maxParticipants must be greater than or equal to 1")
        Integer maxParticipants,

        @NotBlank(message = "difficulty is required")
        String difficulty,

        String duration,
        String meetingPoint,

        @NotNull(message = "startDate is required")
        LocalDateTime startDate,

        @NotNull(message = "endDate is required")
        LocalDateTime endDate
) {

    @AssertTrue(message = "endDate must be after startDate")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }

        return endDate.isAfter(startDate);
    }
}
