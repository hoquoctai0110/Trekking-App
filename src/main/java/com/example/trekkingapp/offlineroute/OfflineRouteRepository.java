package com.example.trekkingapp.offlineroute;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfflineRouteRepository extends JpaRepository<OfflineRoute, Long> {

    Optional<OfflineRoute> findByUser_UserIdAndRoute_RouteId(Long userId, Long routeId);

    List<OfflineRoute> findByUser_UserId(Long userId);
}
