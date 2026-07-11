package com.example.trekkingapp.admin.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    List<SystemSetting> findBySectionOrderBySettingKeyAsc(String section);

    List<SystemSetting> findAllByOrderBySectionAscSettingKeyAsc();

    Optional<SystemSetting> findBySectionAndSettingKey(String section, String settingKey);
}
