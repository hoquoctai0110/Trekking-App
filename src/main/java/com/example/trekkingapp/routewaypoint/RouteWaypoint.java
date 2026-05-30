package com.example.trekkingapp.routewaypoint;

import com.example.trekkingapp.route.Route;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "route_waypoints")
public class RouteWaypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long waypointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    private String name;

    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer orderIndex;

    private Double elevation;

    private Double distanceFromStartKm;

    private Integer estimatedArrivalMinute;

    private Boolean mandatory;

    private String iconKey;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getWaypointId() {
        return waypointId;
    }

    public void setWaypointId(Long waypointId) {
        this.waypointId = waypointId;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public Double getDistanceFromStartKm() {
        return distanceFromStartKm;
    }

    public void setDistanceFromStartKm(Double distanceFromStartKm) {
        this.distanceFromStartKm = distanceFromStartKm;
    }

    public Integer getEstimatedArrivalMinute() {
        return estimatedArrivalMinute;
    }

    public void setEstimatedArrivalMinute(Integer estimatedArrivalMinute) {
        this.estimatedArrivalMinute = estimatedArrivalMinute;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
