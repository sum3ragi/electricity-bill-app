package com.electricity.bill.repository;

import com.electricity.bill.model.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    List<UsageLog> findByUsageDateOrderByCreatedAtDesc(LocalDate date);

    List<UsageLog> findByUsageDateBetweenOrderByUsageDateAscCreatedAtAsc(
            LocalDate start, LocalDate end);

    @Query("SELECT u FROM UsageLog u WHERE u.usageDate BETWEEN :start AND :end ORDER BY u.usageDate ASC")
    List<UsageLog> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<UsageLog> findByApplianceIdOrderByUsageDateDesc(Long applianceId);
}