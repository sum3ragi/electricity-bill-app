package com.electricity.bill.repository;

import com.electricity.bill.model.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplianceRepository extends JpaRepository<Appliance, Long> {
    List<Appliance> findAllByOrderByNameAsc();

    List<Appliance> findByIsPresetTrueOrderByNameAsc();
}
