package com.electricity.bill.repository;

import com.electricity.bill.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends JpaRepository<AppSettings, Long> {}