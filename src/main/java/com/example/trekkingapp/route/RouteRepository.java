package com.example.trekkingapp.route;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByStatusNot(String status);

    Optional<Route> findByRouteIdAndStatusNot(Long routeId, String status);
}
