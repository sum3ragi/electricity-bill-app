package com.electricity.bill.controller;

import com.electricity.bill.model.*;
import com.electricity.bill.service.ApplianceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApplianceController {

    @Autowired private ApplianceService service;

    // ── Settings ─────────────────────────────────────────────────────────────
    @GetMapping("/settings")
    public ResponseEntity<AppSettings> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<AppSettings> saveSettings(@RequestBody AppSettings settings) {
        return ResponseEntity.ok(service.saveSettings(settings));
    }

    // ── Appliances ───────────────────────────────────────────────────────────
    @GetMapping("/appliances")
    public ResponseEntity<List<Appliance>> getAll() {
        return ResponseEntity.ok(service.getAllAppliances());
    }

    @PostMapping("/appliances")
    public ResponseEntity<Appliance> create(@Valid @RequestBody Appliance a) {
        return ResponseEntity.ok(service.saveAppliance(a));
    }

    @PutMapping("/appliances/{id}")
    public ResponseEntity<Appliance> update(@PathVariable Long id, @Valid @RequestBody Appliance a) {
        a.setId(id);
        return ResponseEntity.ok(service.saveAppliance(a));
    }

    @DeleteMapping("/appliances/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteAppliance(id);
        return ResponseEntity.noContent().build();
    }

    // ── Usage Logs ───────────────────────────────────────────────────────────
    @GetMapping("/logs")
    public ResponseEntity<List<UsageLog>> getLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(service.getLogsForDate(d));
    }

    @PostMapping("/logs")
    public ResponseEntity<UsageLog> addLog(@RequestBody Map<String, Object> body) {
        Long applianceId = Long.valueOf(body.get("applianceId").toString());
        LocalDate date = LocalDate.parse(body.get("usageDate").toString());
        Double hours = Double.valueOf(body.get("hoursUsed").toString());
        Integer qty = body.containsKey("quantity") ? Integer.valueOf(body.get("quantity").toString()) : 1;
        String notes = body.containsKey("notes") ? body.get("notes").toString() : null;

        Appliance appliance = service.getApplianceById(applianceId)
                .orElseThrow(() -> new RuntimeException("Appliance not found"));

        UsageLog log = new UsageLog();
        log.setAppliance(appliance);
        log.setUsageDate(date);
        log.setHoursUsed(hours);
        log.setQuantity(qty);
        log.setNotes(notes);

        return ResponseEntity.ok(service.saveUsageLog(log));
    }

    @DeleteMapping("/logs/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        service.deleteUsageLog(id);
        return ResponseEntity.noContent().build();
    }

    // ── Billing Summaries ────────────────────────────────────────────────────
    @GetMapping("/summary/daily")
    public ResponseEntity<Map<String, Object>> dailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.getDailySummary(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/summary/range")
    public ResponseEntity<Map<String, Object>> rangeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.getRangeSummary(start, end));
    }

    @GetMapping("/summary/cycle")
    public ResponseEntity<Map<String, Object>> cycleSummary() {
        return ResponseEntity.ok(service.getCurrentBillingCycleSummary());
    }
}