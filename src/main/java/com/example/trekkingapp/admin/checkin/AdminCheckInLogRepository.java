package com.example.trekkingapp.admin.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminCheckInLogRepository extends JpaRepository<AdminCheckInLog, Long>, JpaSpecificationExecutor<AdminCheckInLog> {
}
