package com.example.trekkingapp.booking;

import com.example.trekkingapp.payment.Payment;
import com.example.trekkingapp.tourschedule.TourSchedule;
import com.example.trekkingapp.tourschedule.TourScheduleRepository;
import com.example.trekkingapp.tourschedule.TourScheduleStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BookingStatusManager {

    private final TourScheduleRepository tourScheduleRepository;

    public BookingStatusManager(TourScheduleRepository tourScheduleRepository) {
        this.tourScheduleRepository = tourScheduleRepository;
    }

    public void markPendingPayment(Booking booking) {
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
    }

    public void markPaid(Booking booking) {
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
    }

    public void markCompleted(Booking booking) {
        booking.setBookingStatus(BookingStatus.COMPLETED);
    }

    public boolean synchronizePaidBooking(Booking booking) {
        Payment payment = booking.getPayment();
        if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
            return false;
        }

        boolean changed = false;
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.PAID);
            changed = true;
        }

        if (booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT
                || booking.getBookingStatus() == BookingStatus.PENDING) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            changed = true;
        }

        return changed;
    }

    public void cancelBooking(Booking booking, PaymentStatus paymentStatus) {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(paymentStatus);
        releaseScheduleSlots(booking);
    }

    public void releaseScheduleSlots(Booking booking) {
        TourSchedule schedule = booking.getSchedule();
        if (schedule == null) {
            return;
        }

        int updatedBookedCount = Math.max(0, schedule.getBookedCount() - booking.getNumberOfPeople());
        schedule.setBookedCount(updatedBookedCount);

        if (schedule.getStatus() == TourScheduleStatus.FULL
                && updatedBookedCount < schedule.getTour().getMaxParticipants()
                && schedule.getStartDateTime().isAfter(LocalDateTime.now())) {
            schedule.setStatus(TourScheduleStatus.OPEN);
        }

        tourScheduleRepository.save(schedule);
    }
}
