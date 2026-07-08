package com.example.trekkingapp.trekkerprofile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrekkerProfileRepository extends JpaRepository<TrekkerProfile, Long> {

    Optional<TrekkerProfile> findByUser_UserId(Long userId);

    boolean existsByUser_UserId(Long userId);
}
