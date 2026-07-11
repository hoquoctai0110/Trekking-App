package com.example.trekkingapp.admin.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminCheckInTokenRepository extends JpaRepository<AdminCheckInToken, Long> {

    Optional<AdminCheckInToken> findFirstByTour_TourIdAndSchedule_ScheduleIdAndRevokedAtIsNullOrderByVersionDesc(Long tourId, Long scheduleId);

    Optional<AdminCheckInToken> findFirstByTour_TourIdAndRevokedAtIsNullOrderByVersionDesc(Long tourId);
}
