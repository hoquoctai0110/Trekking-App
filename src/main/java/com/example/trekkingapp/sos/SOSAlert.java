package com.example.trekkingapp.sos;

import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tracking.TrackingSession;
import com.example.trekkingapp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sos_alerts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sos_alerts_user_client_request",
                columnNames = {"user_id", "client_request_id"}
        )
)
public class SOSAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sosId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_session_id")
    private TrackingSession trackingSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SOSAlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SOSAlertSource source;

    @Column(length = 100)
    private String clientRequestId;

    @Column(nullable = false)
    private LocalDateTime clientCreatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = SOSAlertStatus.PENDING;
        }
        if (source == null) {
            source = SOSAlertSource.API;
        }
        if (clientCreatedAt == null) {
            clientCreatedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getSosId() {
        return sosId;
    }

    public void setSosId(Long sosId) {
        this.sosId = sosId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public TrackingSession getTrackingSession() {
        return trackingSession;
    }

    public void setTrackingSession(TrackingSession trackingSession) {
        this.trackingSession = trackingSession;
    }

    public Tour getTour() {
        return tour;
    }

    public void setTour(Tour tour) {
        this.tour = tour;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SOSAlertStatus getStatus() {
        return status;
    }

    public void setStatus(SOSAlertStatus status) {
        this.status = status;
    }

    public SOSAlertSource getSource() {
        return source;
    }

    public void setSource(SOSAlertSource source) {
        this.source = source;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public LocalDateTime getClientCreatedAt() {
        return clientCreatedAt;
    }

    public void setClientCreatedAt(LocalDateTime clientCreatedAt) {
        this.clientCreatedAt = clientCreatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
