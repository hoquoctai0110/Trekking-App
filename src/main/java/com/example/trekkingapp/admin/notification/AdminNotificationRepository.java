package com.example.trekkingapp.admin.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long>, JpaSpecificationExecutor<AdminNotification> {

    Optional<AdminNotification> findByIdempotencyKey(String idempotencyKey);
}
