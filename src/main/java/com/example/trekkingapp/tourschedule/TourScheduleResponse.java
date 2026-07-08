package com.example.trekkingapp.tourschedule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TourScheduleResponse(
        Long scheduleId,
        Long tourId,
        String tourTitle,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        BigDecimal tourPrice,
        Integer maxParticipants,
        Integer bookedCount,
        Integer availableSlots,
        TourScheduleStatus status
) {
}
