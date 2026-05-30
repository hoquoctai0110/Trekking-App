package com.example.trekkingapp.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull(message = "tourId is required")
        Long tourId,

        @NotNull(message = "numberOfPeople is required")
        @Min(value = 1, message = "numberOfPeople must be greater than or equal to 1")
        Integer numberOfPeople
) {
}
