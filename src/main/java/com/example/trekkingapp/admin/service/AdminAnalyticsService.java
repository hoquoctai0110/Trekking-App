package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.dto.response.AdminAnalyticsOverviewResponse;
import com.example.trekkingapp.admin.dto.response.AdminDashboardSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminRevenueChartItemResponse;
import com.example.trekkingapp.admin.dto.response.AdminTopTourResponse;
import com.example.trekkingapp.common.ValidationException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminAnalyticsService {

    private final EntityManager entityManager;
    private final AdminDashboardService adminDashboardService;

    public AdminAnalyticsService(EntityManager entityManager, AdminDashboardService adminDashboardService) {
        this.entityManager = entityManager;
        this.adminDashboardService = adminDashboardService;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsOverviewResponse overview(LocalDate dateFrom, LocalDate dateTo) {
        validateRange(dateFrom, dateTo);
        LocalDateTime start = dateFrom == null ? LocalDate.now().minusMonths(1).atStartOfDay() : dateFrom.atStartOfDay();
        LocalDateTime end = dateTo == null ? LocalDateTime.now() : dateTo.plusDays(1).atStartOfDay();

        BigDecimal totalRevenue = entityManager.createQuery("""
                select coalesce(sum(p.amount), 0) from Payment p
                where p.status = com.example.trekkingapp.booking.PaymentStatus.PAID and p.createdAt >= :start and p.createdAt < :end
                """, BigDecimal.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        Long totalBookings = entityManager.createQuery("""
                select count(b) from Booking b where b.bookedAt >= :start and b.bookedAt < :end
                """, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        Long completedBookings = entityManager.createQuery("""
                select count(b) from Booking b where b.bookingStatus = com.example.trekkingapp.booking.BookingStatus.COMPLETED and b.bookedAt >= :start and b.bookedAt < :end
                """, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        Long cancelledBookings = entityManager.createQuery("""
                select count(b) from Booking b where b.bookingStatus = com.example.trekkingapp.booking.BookingStatus.CANCELLED and b.bookedAt >= :start and b.bookedAt < :end
                """, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        Long newUsers = entityManager.createQuery("""
                select count(u) from User u where u.createdAt >= :start and u.createdAt < :end
                """, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        Long newProviders = entityManager.createQuery("""
                select count(tp) from TourProvider tp where tp.createdAt >= :start and tp.createdAt < :end
                """, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        Long newTours = entityManager.createQuery("""
                select count(t) from Tour t where t.createdAt >= :start and t.createdAt < :end
                """, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();

        BigDecimal averageBookingValue = totalBookings == 0 ? BigDecimal.ZERO : totalRevenue.divide(BigDecimal.valueOf(totalBookings), java.math.RoundingMode.HALF_UP);
        return new AdminAnalyticsOverviewResponse(totalRevenue, totalBookings, completedBookings, cancelledBookings, newUsers, newProviders, newTours, averageBookingValue, null);
    }

    @Transactional(readOnly = true)
    public List<AdminRevenueChartItemResponse> revenueBookings(String period) {
        return adminDashboardService.revenue(period);
    }

    @Transactional(readOnly = true)
    public List<AdminTopTourResponse> topTours() {
        return adminDashboardService.topTours(10);
    }

    @Transactional(readOnly = true)
    public List<AdminDashboardSummaryResponse.TrendPoint> newUsers() {
        return adminDashboardService.summary().userTrend();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(LocalDate dateFrom, LocalDate dateTo) {
        AdminAnalyticsOverviewResponse overview = overview(dateFrom, dateTo);
        String csv = """
                metric,value
                totalRevenue,%s
                totalBookings,%s
                completedBookings,%s
                cancelledBookings,%s
                newUsers,%s
                newTourProviders,%s
                newTours,%s
                averageBookingValue,%s
                """.formatted(
                overview.totalRevenue(),
                overview.totalBookings(),
                overview.completedBookings(),
                overview.cancelledBookings(),
                overview.newUsers(),
                overview.newTourProviders(),
                overview.newTours(),
                overview.averageBookingValue()
        );
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private void validateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ValidationException("dateFrom must be before or equal to dateTo");
        }
    }
}
