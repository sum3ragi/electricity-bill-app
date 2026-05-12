package com.electricity.bill.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appliance_id", nullable = false)
    private Appliance appliance;

    @NotNull
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    /** Hours used per day */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("24.0")
    @Column(name = "hours_used", nullable = false)
    private Double hoursUsed;

    /** Number of units of this appliance running */
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Appliance getAppliance() {
        return appliance;
    }

    public void setAppliance(Appliance appliance) {
        this.appliance = appliance;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public Double getHoursUsed() {
        return hoursUsed;
    }

    public void setHoursUsed(Double hoursUsed) {
        this.hoursUsed = hoursUsed;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** kWh = (watts × hours × quantity) / 1000 */
    @Transient
    public Double getKwhConsumed() {
        return (appliance.getWatts() * hoursUsed * quantity) / 1000.0;
    }

    /** Cost = kWh × rate */
    @Transient
    public Double getCost(double ratePerKwh) {
        return getKwhConsumed() * ratePerKwh;
    }
}
