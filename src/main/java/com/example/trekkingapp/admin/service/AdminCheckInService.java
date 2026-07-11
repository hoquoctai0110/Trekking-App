package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.AdminSortUtils;
import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.checkin.AdminCheckInLog;
import com.example.trekkingapp.admin.checkin.AdminCheckInLogRepository;
import com.example.trekkingapp.admin.checkin.AdminCheckInToken;
import com.example.trekkingapp.admin.checkin.AdminCheckInTokenRepository;
import com.example.trekkingapp.admin.dto.request.AdminCheckInQrRequest;
import com.example.trekkingapp.admin.dto.response.AdminCheckInResponse;
import com.example.trekkingapp.admin.dto.response.AdminCheckInSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminGeneratedQrResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.specification.AdminCheckInSpecification;
import com.example.trekkingapp.common.ResourceNotFoundException;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourschedule.TourSchedule;
import com.example.trekkingapp.tourschedule.TourScheduleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminCheckInService {

    private static final Set<String> SORT_FIELDS = Set.of("checkInTime", "status");

    private final AdminCheckInLogRepository adminCheckInLogRepository;
    private final AdminCheckInTokenRepository adminCheckInTokenRepository;
    private final TourRepository tourRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final AdminAuditService adminAuditService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminCheckInService(
            AdminCheckInLogRepository adminCheckInLogRepository,
            AdminCheckInTokenRepository adminCheckInTokenRepository,
            TourRepository tourRepository,
            TourScheduleRepository tourScheduleRepository,
            AdminAuditService adminAuditService
    ) {
        this.adminCheckInLogRepository = adminCheckInLogRepository;
        this.adminCheckInTokenRepository = adminCheckInTokenRepository;
        this.tourRepository = tourRepository;
        this.tourScheduleRepository = tourScheduleRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCheckInResponse> findCheckIns(int page, int size, String sort, String search, Long tourId, Long scheduleId, String status, LocalDate date) {
        Pageable pageable = AdminSortUtils.pageable(page, size, sort, "checkInTime", SORT_FIELDS);
        Specification<AdminCheckInLog> specification = Specification
                .where(AdminCheckInSpecification.search(search))
                .and(AdminCheckInSpecification.tourId(tourId))
                .and(AdminCheckInSpecification.scheduleId(scheduleId))
                .and(AdminCheckInSpecification.status(status))
                .and(AdminCheckInSpecification.date(date));
        return PageResponse.from(adminCheckInLogRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminCheckInSummaryResponse summary() {
        long total = adminCheckInLogRepository.count();
        long success = adminCheckInLogRepository.count(AdminCheckInSpecification.status("SUCCESS"));
        long failed = adminCheckInLogRepository.count(AdminCheckInSpecification.status("FAILED"));
        return new AdminCheckInSummaryResponse(total, success, failed);
    }

    @Transactional
    public AdminGeneratedQrResponse generate(AdminCheckInQrRequest request) {
        Tour tour = tourRepository.findById(request.tourId()).orElseThrow(() -> new ResourceNotFoundException("Tour not found"));
        TourSchedule schedule = request.scheduleId() == null ? null : tourScheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        String rawToken = UUID.randomUUID() + ":" + System.currentTimeMillis();

        AdminCheckInToken token = new AdminCheckInToken();
        token.setTour(tour);
        token.setSchedule(schedule);
        token.setTokenHash(encoder.encode(rawToken));
        token.setVersion(1);
        token.setExpiresAt(request.expiresAt());
        AdminCheckInToken saved = adminCheckInTokenRepository.save(token);
        adminAuditService.log("GENERATE_CHECKIN_QR", "CHECKIN_TOKEN", saved.getId(), null, "GENERATED", null);
        return new AdminGeneratedQrResponse(saved.getId(), tour.getTourId(), schedule == null ? null : schedule.getScheduleId(), encodeToken(rawToken), saved.getVersion(), saved.getExpiresAt());
    }

    @Transactional
    public AdminGeneratedQrResponse regenerate(AdminCheckInQrRequest request) {
        Tour tour = tourRepository.findById(request.tourId()).orElseThrow(() -> new ResourceNotFoundException("Tour not found"));
        TourSchedule schedule = request.scheduleId() == null ? null : tourScheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        AdminCheckInToken current = request.scheduleId() == null
                ? adminCheckInTokenRepository.findFirstByTour_TourIdAndRevokedAtIsNullOrderByVersionDesc(request.tourId()).orElse(null)
                : adminCheckInTokenRepository.findFirstByTour_TourIdAndSchedule_ScheduleIdAndRevokedAtIsNullOrderByVersionDesc(request.tourId(), request.scheduleId()).orElse(null);
        int nextVersion = 1;
        if (current != null) {
            current.setRevokedAt(LocalDateTime.now());
            adminCheckInTokenRepository.save(current);
            nextVersion = current.getVersion() + 1;
        }

        String rawToken = UUID.randomUUID() + ":" + System.currentTimeMillis();
        AdminCheckInToken token = new AdminCheckInToken();
        token.setTour(tour);
        token.setSchedule(schedule);
        token.setTokenHash(encoder.encode(rawToken));
        token.setVersion(nextVersion);
        token.setExpiresAt(request.expiresAt());
        AdminCheckInToken saved = adminCheckInTokenRepository.save(token);
        adminAuditService.log("REGENERATE_CHECKIN_QR", "CHECKIN_TOKEN", saved.getId(), null, "VERSION_" + nextVersion, null);
        return new AdminGeneratedQrResponse(saved.getId(), tour.getTourId(), schedule == null ? null : schedule.getScheduleId(), encodeToken(rawToken), saved.getVersion(), saved.getExpiresAt());
    }

    private String encodeToken(String rawToken) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken.getBytes());
    }

    private AdminCheckInResponse toResponse(AdminCheckInLog log) {
        return new AdminCheckInResponse(
                log.getId(),
                log.getUser().getFullName(),
                log.getTour().getTitle(),
                log.getCheckpoint(),
                log.getCheckInTime(),
                log.getStatus(),
                log.getLatitude(),
                log.getLongitude()
        );
    }
}
