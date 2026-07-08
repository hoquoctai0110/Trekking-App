package com.example.trekkingapp.tour;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Long> {

    List<Tour> findByStatus(String status);

    List<Tour> findByStatusNot(String status);

    List<Tour> findByProvider_ProviderIdAndStatusNot(Long providerId, String status);

    Optional<Tour> findByTourIdAndStatusNot(Long tourId, String status);
}
