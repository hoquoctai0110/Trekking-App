package com.example.trekkingapp.sos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SOSAlertRepository extends JpaRepository<SOSAlert, Long> {

    Optional<SOSAlert> findBySosId(Long sosId);

    Optional<SOSAlert> findByUser_UserIdAndClientRequestId(Long userId, String clientRequestId);

    List<SOSAlert> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    List<SOSAlert> findByTour_Provider_ProviderIdOrderByCreatedAtDesc(Long providerId);

    List<SOSAlert> findAllByOrderByCreatedAtDesc();
}
