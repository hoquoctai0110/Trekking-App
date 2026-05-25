package com.example.trekkingapp.tourprovider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourProviderRepository extends JpaRepository<TourProvider, Long> {

    Optional<TourProvider> findByUser_UserId(Long userId);

    boolean existsByUser_UserId(Long userId);

    List<TourProvider> findByStatusNot(String status);
}
