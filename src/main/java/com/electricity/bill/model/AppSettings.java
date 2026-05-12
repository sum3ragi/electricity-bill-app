package com.electricity.bill.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "app_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSettings {

    @Id
    private Long id = 1L; // Singleton row

    /** Meralco rate per kWh */
    @Column(name = "rate_per_kwh", nullable = false)
    private Double ratePerKwh = 12.50;

    /** Day of month the billing cycle starts (1-28) */
    @Column(name = "billing_cycle_start", nullable = false)
    private Integer billingCycleStart = 1;

    @Column(name = "household_name")
    private String householdName = "My Household";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getRatePerKwh() {
        return ratePerKwh;
    }

    public void setRatePerKwh(Double ratePerKwh) {
        this.ratePerKwh = ratePerKwh;
    }

    public Integer getBillingCycleStart() {
        return billingCycleStart;
    }

    public void setBillingCycleStart(Integer billingCycleStart) {
        this.billingCycleStart = billingCycleStart;
    }

    public String getHouseholdName() {
        return householdName;
    }

    public void setHouseholdName(String householdName) {
        this.householdName = householdName;
    }
}
