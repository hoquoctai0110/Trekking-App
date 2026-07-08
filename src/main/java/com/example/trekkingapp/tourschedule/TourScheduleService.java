package com.example.trekkingapp.tourschedule;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TourScheduleService {

    private static final String TOUR_STATUS_DELETED = "DELETED";
    private static final String ROLE_ADMIN = "ADMIN";

    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;
    private final TourProviderRepository tourProviderRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public TourScheduleService(
            TourScheduleRepository tourScheduleRepository,
            TourRepository tourRepository,
            TourProviderRepository tourProviderRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.tourScheduleRepository = tourScheduleRepository;
        this.tourRepository = tourRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TourScheduleResponse createSchedule(Long tourId, TourScheduleRequest request) {
        Tour tour = findActiveTour(tourId);
        validateTourAccess(tour);

        TourSchedule schedule = new TourSchedule();
        schedule.setTour(tour);
        schedule.setBookedCount(0);
        applyRequest(schedule, request);

        return toResponse(tourScheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<TourScheduleResponse> findAvailableSchedules(Long tourId) {
        findActiveTour(tourId);
        return tourScheduleRepository.findByTour_TourIdAndStatusAndStartDateTimeAfter(
                        tourId,
                        TourScheduleStatus.OPEN,
                        LocalDateTime.now()
                ).stream()
                .filter(schedule -> schedule.getAvailableSlots() > 0)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TourScheduleResponse> findMySchedules(Long tourId) {
        Tour tour = findActiveTour(tourId);
        validateTourAccess(tour);

        return tourScheduleRepository.findByTour_TourId(tourId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TourScheduleResponse updateSchedule(Long tourId, Long scheduleId, TourScheduleRequest request) {
        Tour tour = findActiveTour(tourId);
        validateTourAccess(tour);

        TourSchedule schedule = findSchedule(scheduleId, tourId);
        applyRequest(schedule, request);
        validateScheduleState(schedule);

        return toResponse(tourScheduleRepository.save(schedule));
    }

    @Transactional
    public String cancelSchedule(Long tourId, Long scheduleId) {
        Tour tour = findActiveTour(tourId);
        validateTourAccess(tour);

        TourSchedule schedule = findSchedule(scheduleId, tourId);
        schedule.setStatus(TourScheduleStatus.CANCELLED);
        tourScheduleRepository.save(schedule);

        return "Tour schedule cancelled successfully";
    }

    @Transactional(readOnly = true)
    public TourSchedule findOpenScheduleForBooking(Long tourId, Long scheduleId) {
        TourSchedule schedule = findSchedule(scheduleId, tourId);
        if (schedule.getStatus() != TourScheduleStatus.OPEN) {
            throw new IllegalArgumentException("Tour schedule is not open for booking");
        }
        if (schedule.getAvailableSlots() <= 0) {
            throw new IllegalArgumentException("Tour schedule has no available slots");
        }
        return schedule;
    }

    private Tour findActiveTour(Long tourId) {
        return tourRepository.findByTourIdAndStatusNot(tourId, TOUR_STATUS_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));
    }

    private TourSchedule findSchedule(Long scheduleId, Long tourId) {
        return tourScheduleRepository.findByScheduleIdAndTour_TourId(scheduleId, tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour schedule not found"));
    }

    private void applyRequest(TourSchedule schedule, TourScheduleRequest request) {
        schedule.setStartDateTime(request.startDateTime());
        schedule.setEndDateTime(request.endDateTime());
        schedule.setStatus(resolveStatus(request.status(), schedule));
        validateScheduleState(schedule);
    }

    private TourScheduleStatus resolveStatus(TourScheduleStatus requestedStatus, TourSchedule schedule) {
        if (requestedStatus == null) {
            return schedule.getStatus() == null ? TourScheduleStatus.OPEN : schedule.getStatus();
        }

        if (requestedStatus == TourScheduleStatus.OPEN
                && schedule.getBookedCount() >= schedule.getTour().getMaxParticipants()) {
            return TourScheduleStatus.FULL;
        }

        return requestedStatus;
    }

    private void validateScheduleState(TourSchedule schedule) {
        if (!schedule.getEndDateTime().isAfter(schedule.getStartDateTime())) {
            throw new IllegalArgumentException("endDateTime must be after startDateTime");
        }
        if (schedule.getBookedCount() == null || schedule.getBookedCount() < 0) {
            throw new IllegalArgumentException("bookedCount must be greater than or equal to 0");
        }
        if (schedule.getBookedCount() > schedule.getTour().getMaxParticipants()) {
            throw new IllegalArgumentException("bookedCount cannot exceed maxParticipants");
        }
        if (schedule.getBookedCount() >= schedule.getTour().getMaxParticipants()
                && schedule.getStatus() == TourScheduleStatus.OPEN) {
            schedule.setStatus(TourScheduleStatus.FULL);
        }
    }

    private void validateTourAccess(Tour tour) {
        User currentUser = findCurrentUser();
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        TourProvider provider = tourProviderRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Tour provider profile not found"));

        if (!tour.getProvider().getProviderId().equals(provider.getProviderId())) {
            throw new IllegalArgumentException("You are not allowed to modify this tour");
        }
    }

    private User findCurrentUser() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getUserRoles()
                .stream()
                .anyMatch(userRole -> roleName.equals(userRole.getRole().getRoleName()));
    }

    private TourScheduleResponse toResponse(TourSchedule schedule) {
        return new TourScheduleResponse(
                schedule.getScheduleId(),
                schedule.getTour().getTourId(),
                schedule.getTour().getTitle(),
                schedule.getStartDateTime(),
                schedule.getEndDateTime(),
                schedule.getTour().getPrice(),
                schedule.getTour().getMaxParticipants(),
                schedule.getBookedCount(),
                schedule.getAvailableSlots(),
                schedule.getStatus()
        );
    }
}
