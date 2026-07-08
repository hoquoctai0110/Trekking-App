package com.example.trekkingapp.tourschedule;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TourScheduleRepository extends JpaRepository<TourSchedule, Long> {

    List<TourSchedule> findByTour_TourIdAndStatusAndStartDateTimeAfter(
            Long tourId,
            TourScheduleStatus status,
            LocalDateTime startDateTime
    );

    List<TourSchedule> findByTour_TourId(Long tourId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TourSchedule> findByScheduleIdAndTour_TourId(Long scheduleId, Long tourId);
}
