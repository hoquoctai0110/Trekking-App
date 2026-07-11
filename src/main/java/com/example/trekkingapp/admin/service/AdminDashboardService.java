package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.dto.response.AdminBookingStatusSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminDashboardSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminRecentActivityResponse;
import com.example.trekkingapp.admin.dto.response.AdminRevenueChartItemResponse;
import com.example.trekkingapp.admin.dto.response.AdminTopTourResponse;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.PaymentStatus;
import com.example.trekkingapp.review.ReviewRepository;
import com.example.trekkingapp.sos.SOSAlertRepository;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.UserRepository;
import com.example.trekkingapp.admin.audit.AdminAuditLogRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final TourProviderRepository tourProviderRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final SOSAlertRepository sosAlertRepository;
    private final ReviewRepository reviewRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final EntityManager entityManager;

    public AdminDashboardService(
            UserRepository userRepository,
            TourProviderRepository tourProviderRepository,
            TourRepository tourRepository,
            BookingRepository bookingRepository,
            SOSAlertRepository sosAlertRepository,
            ReviewRepository reviewRepository,
            AdminAuditLogRepository adminAuditLogRepository,
            EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.tourRepository = tourRepository;
        this.bookingRepository = bookingRepository;
        this.sosAlertRepository = sosAlertRepository;
        this.reviewRepository = reviewRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse summary() {
        return new AdminDashboardSummaryResponse(
                userRepository.count(),
                userRepository.count(AdminUserStatistics.activeUsers()),
                tourProviderRepository.count(),
                tourRepository.count(),
                tourRepository.count((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "PUBLISHED")),
                tourRepository.count((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "DRAFT")),
                bookingRepository.count((root, query, cb) -> cb.or(cb.equal(root.get("bookingStatus"), BookingStatus.PENDING_PAYMENT), cb.equal(root.get("bookingStatus"), BookingStatus.CONFIRMED))),
                bookingRepository.count((root, query, cb) -> cb.equal(root.get("bookingStatus"), BookingStatus.COMPLETED)),
                paidRevenueForMonth(YearMonth.now()),
                tourProviderRepository.count((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "PENDING")) + tourRepository.count((root, query, cb) -> cb.equal(cb.upper(root.get("status")), "DRAFT")),
                sosAlertRepository.count((root, query, cb) -> cb.equal(cb.upper(root.get("status").as(String.class)), "PENDING")),
                buildUserTrend(),
                buildRevenueTrend(),
                buildBookingTrend()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminRevenueChartItemResponse> revenue(String period) {
        if ("year".equalsIgnoreCase(period)) {
            return buildRevenueItems(12);
        }
        if ("quarter".equalsIgnoreCase(period)) {
            return buildRevenueItems(3);
        }
        return buildRevenueItems(1);
    }

    @Transactional(readOnly = true)
    public AdminBookingStatusSummaryResponse bookingStatus() {
        return new AdminBookingStatusSummaryResponse(
                bookingRepository.count((root, query, cb) -> cb.or(cb.equal(root.get("bookingStatus"), BookingStatus.PENDING_PAYMENT), cb.equal(root.get("bookingStatus"), BookingStatus.PENDING))),
                bookingRepository.count((root, query, cb) -> cb.equal(root.get("bookingStatus"), BookingStatus.CONFIRMED)),
                0,
                bookingRepository.count((root, query, cb) -> cb.equal(root.get("bookingStatus"), BookingStatus.COMPLETED)),
                bookingRepository.count((root, query, cb) -> cb.equal(root.get("bookingStatus"), BookingStatus.CANCELLED))
        );
    }

    @Transactional(readOnly = true)
    public List<AdminTopTourResponse> topTours(int limit) {
        List<Object[]> rows = entityManager.createQuery("""
                select b.tour.tourId, b.tour.title, count(b.bookingId)
                from Booking b
                group by b.tour.tourId, b.tour.title
                order by count(b.bookingId) desc
                """, Object[].class)
                .setMaxResults(limit <= 0 ? 5 : limit)
                .getResultList();
        return rows.stream()
                .map(row -> new AdminTopTourResponse(
                        (Long) row[0],
                        (String) row[1],
                        (Long) row[2],
                        reviewRepository.findAverageRatingByTourId((Long) row[0]).orElse(0.0),
                        revenueByTour((Long) row[0])
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminRecentActivityResponse> recentActivities() {
        return adminAuditLogRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(log -> new AdminRecentActivityResponse(log.getId(), log.getAction(), log.getEntityType(), log.getEntityId(), log.getReason(), log.getCreatedAt()))
                .toList();
    }

    private BigDecimal paidRevenueForMonth(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        BigDecimal revenue = entityManager.createQuery("""
                select coalesce(sum(p.amount), 0)
                from Payment p
                where p.status = :status and p.createdAt >= :start and p.createdAt < :end
                """, BigDecimal.class)
                .setParameter("status", PaymentStatus.PAID)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        return revenue == null ? BigDecimal.ZERO : revenue;
    }

    private BigDecimal revenueByTour(Long tourId) {
        BigDecimal revenue = entityManager.createQuery("""
                select coalesce(sum(p.amount), 0)
                from Payment p
                where p.status = :status and p.booking.tour.tourId = :tourId
                """, BigDecimal.class)
                .setParameter("status", PaymentStatus.PAID)
                .setParameter("tourId", tourId)
                .getSingleResult();
        return revenue == null ? BigDecimal.ZERO : revenue;
    }

    private List<AdminDashboardSummaryResponse.TrendPoint> buildUserTrend() {
        List<AdminDashboardSummaryResponse.TrendPoint> points = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = userRepository.count((root, query, cb) -> cb.between(root.get("createdAt"), month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()));
            points.add(new AdminDashboardSummaryResponse.TrendPoint(month.toString(), count));
        }
        return points;
    }

    private List<AdminDashboardSummaryResponse.TrendPoint> buildRevenueTrend() {
        List<AdminDashboardSummaryResponse.TrendPoint> points = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            points.add(new AdminDashboardSummaryResponse.TrendPoint(month.toString(), paidRevenueForMonth(month)));
        }
        return points;
    }

    private List<AdminDashboardSummaryResponse.TrendPoint> buildBookingTrend() {
        List<AdminDashboardSummaryResponse.TrendPoint> points = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = bookingRepository.count((root, query, cb) -> cb.between(root.get("bookedAt"), month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()));
            points.add(new AdminDashboardSummaryResponse.TrendPoint(month.toString(), count));
        }
        return points;
    }

    private List<AdminRevenueChartItemResponse> buildRevenueItems(int months) {
        List<AdminRevenueChartItemResponse> items = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
            long bookings = bookingRepository.count((root, query, cb) -> cb.between(root.get("bookedAt"), start, end));
            long newUsers = userRepository.count((root, query, cb) -> cb.between(root.get("createdAt"), start, end));
            items.add(new AdminRevenueChartItemResponse(month.toString(), paidRevenueForMonth(month), bookings, newUsers));
        }
        return items;
    }

    private static final class AdminUserStatistics {
        private static org.springframework.data.jpa.domain.Specification<com.example.trekkingapp.user.User> activeUsers() {
            return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), "ACTIVE");
        }
    }
}
