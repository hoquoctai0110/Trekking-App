package com.example.trekkingapp.tourschedule;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TourScheduleRequest(
        @NotNull(message = "startDateTime is required")
        LocalDateTime startDateTime,

        @NotNull(message = "endDateTime is required")
        LocalDateTime endDateTime,

        TourScheduleStatus status
) {

    @AssertTrue(message = "endDateTime must be after startDateTime")
    public boolean isEndDateTimeAfterStartDateTime() {
        if (startDateTime == null || endDateTime == null) {
            return true;
        }

        return endDateTime.isAfter(startDateTime);
    }
}
