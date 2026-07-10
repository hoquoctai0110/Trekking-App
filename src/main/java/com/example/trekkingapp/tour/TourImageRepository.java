package com.example.trekkingapp.tour;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourImageRepository extends JpaRepository<TourImage, Long> {

    List<TourImage> findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(Long tourId);

    Optional<TourImage> findByImageIdAndTour_TourId(Long imageId, Long tourId);
}
