package com.example.trekkingapp.tourprovider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourProviderRepository extends JpaRepository<TourProvider, Long> {

    List<TourProvider> findByStatusNot(String status);

    Optional<TourProvider> findByProviderIdAndStatusNot(Long providerId, String status);
}
