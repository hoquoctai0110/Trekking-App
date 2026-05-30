package com.example.trekkingapp.offlineroute;

import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "offline_routes",
        uniqueConstraints = @UniqueConstraint(name = "uk_offline_routes_user_route", columnNames = {"user_id", "route_id"})
)
public class OfflineRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long offlineRouteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    private LocalDateTime downloadedAt;

    private LocalDateTime lastSyncedAt;

    private String localVersion;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        downloadedAt = now;
        lastSyncedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        lastSyncedAt = LocalDateTime.now();
    }

    public Long getOfflineRouteId() {
        return offlineRouteId;
    }

    public void setOfflineRouteId(Long offlineRouteId) {
        this.offlineRouteId = offlineRouteId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public LocalDateTime getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(LocalDateTime downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getLocalVersion() {
        return localVersion;
    }

    public void setLocalVersion(String localVersion) {
        this.localVersion = localVersion;
    }
}
