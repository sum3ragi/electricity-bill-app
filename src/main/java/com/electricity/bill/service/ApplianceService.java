package com.electricity.bill.service;

import com.electricity.bill.model.*;
import com.electricity.bill.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplianceService {

    @Autowired private ApplianceRepository applianceRepo;
    @Autowired private UsageLogRepository usageLogRepo;
    @Autowired private SettingsRepository settingsRepo;

    // ─── SETTINGS ─────────────────────────────────────────────────────────────

    public AppSettings getSettings() {
        return settingsRepo.findById(1L).orElseGet(() -> {
            AppSettings s = new AppSettings();
            s.setId(1L);
            return settingsRepo.save(s);
        });
    }

    public AppSettings saveSettings(AppSettings settings) {
        settings.setId(1L);
        return settingsRepo.save(settings);
    }

    // ─── PRESETS SEED ─────────────────────────────────────────────────────────

    @PostConstruct
    public void seedPresets() {
        if (applianceRepo.findByIsPresetTrueOrderByNameAsc().isEmpty()) {
            List<Appliance> presets = Arrays.asList(
                preset("Air Conditioner (1HP)",  746.0,  "❄️",  "Cooling"),
                preset("Air Conditioner (1.5HP)",1119.0, "❄️",  "Cooling"),
                preset("Air Conditioner (2HP)",  1492.0, "❄️",  "Cooling"),
                preset("Electric Fan",            60.0,  "🌀",  "Cooling"),
                preset("Ceiling Fan",             75.0,  "🌀",  "Cooling"),
                preset("Refrigerator (small)",   100.0,  "🧊",  "Kitchen"),
                preset("Refrigerator (medium)",  150.0,  "🧊",  "Kitchen"),
                preset("Refrigerator (large)",   200.0,  "🧊",  "Kitchen"),
                preset("Rice Cooker",            700.0,  "🍚",  "Kitchen"),
                preset("Microwave Oven",        1200.0,  "📦",  "Kitchen"),
                preset("Electric Stove",        2000.0,  "🔥",  "Kitchen"),
                preset("Water Heater",          1500.0,  "🚿",  "Bathroom"),
                preset("Washing Machine",        500.0,  "🫧",  "Laundry"),
                preset("Clothes Dryer",         2000.0,  "🫧",  "Laundry"),
                preset("LED TV (32\")",           50.0,  "📺",  "Entertainment"),
                preset("LED TV (55\")",          100.0,  "📺",  "Entertainment"),
                preset("Desktop Computer",       200.0,  "💻",  "Entertainment"),
                preset("Laptop",                  65.0,  "💻",  "Entertainment"),
                preset("Game Console",           150.0,  "🎮",  "Entertainment"),
                preset("LED Bulb (9W)",            9.0,  "💡",  "Lighting"),
                preset("Fluorescent Light",       40.0,  "💡",  "Lighting"),
                preset("Electric Iron",         1000.0,  "👔",  "Others"),
                preset("Water Pump",             370.0,  "💧",  "Others"),
                preset("CCTV Camera",              5.0,  "📷",  "Others"),
                preset("WiFi Router",              10.0, "📡",  "Others")
            );
            if (presets != null) {
                applianceRepo.saveAll(presets);
            }
        }
    }

    private Appliance preset(String name, Double watts, String icon, String category) {
        Appliance a = new Appliance();
        a.setName(name); a.setWatts(watts);
        a.setIcon(icon); a.setCategory(category);
        a.setIsPreset(true);
        return a;
    }

    // ─── APPLIANCES ───────────────────────────────────────────────────────────

    public List<Appliance> getAllAppliances() {
        return applianceRepo.findAllByOrderByNameAsc();
    }

    public Appliance saveAppliance(Appliance a) {
        a.setIsPreset(false);
        return applianceRepo.save(a);
    }

    public void deleteAppliance(Long id) {
        if (id != null) {
            applianceRepo.deleteById(id);
        }
    }

    public Optional<Appliance> getApplianceById(Long id) {
        if (id != null) {
            return applianceRepo.findById(id);
        }
        return Optional.empty();
    }

    // ─── USAGE LOGS ───────────────────────────────────────────────────────────

    public UsageLog saveUsageLog(UsageLog log) {
        if (log != null) {
            return usageLogRepo.save(log);
        }
        return null;
    }

    public List<UsageLog> getLogsForDate(LocalDate date) {
        return usageLogRepo.findByUsageDateOrderByCreatedAtDesc(date);
    }

    public List<UsageLog> getLogsForRange(LocalDate start, LocalDate end) {
        return usageLogRepo.findByDateRange(start, end);
    }

    public void deleteUsageLog(Long id) {
        if (id != null) {
            usageLogRepo.deleteById(id);
        }
    }

    // ─── BILLING SUMMARY ──────────────────────────────────────────────────────

    public Map<String, Object> getDailySummary(LocalDate date) {
        double rate = getSettings().getRatePerKwh();
        List<UsageLog> logs = getLogsForDate(date);
        return buildSummary(logs, rate, date.toString(), date.toString());
    }

    public Map<String, Object> getRangeSummary(LocalDate start, LocalDate end) {
        double rate = getSettings().getRatePerKwh();
        List<UsageLog> logs = getLogsForRange(start, end);
        return buildSummary(logs, rate, start.toString(), end.toString());
    }

    public Map<String, Object> getCurrentBillingCycleSummary() {
        AppSettings settings = getSettings();
        int cycleDay = settings.getBillingCycleStart();
        double rate = settings.getRatePerKwh();

        LocalDate today = LocalDate.now();
        LocalDate cycleStart;
        if (today.getDayOfMonth() >= cycleDay) {
            cycleStart = today.withDayOfMonth(cycleDay);
        } else {
            cycleStart = today.minusMonths(1).withDayOfMonth(cycleDay);
        }

        List<UsageLog> logs = getLogsForRange(cycleStart, today);
        Map<String, Object> summary = buildSummary(logs, rate, cycleStart.toString(), today.toString());
        summary.put("cycleStart", cycleStart.toString());
        summary.put("cycleEnd", today.toString());

        // Project to end of month
        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(cycleStart, today) + 1;
        long totalDays = 30;
        double currentKwh = (double) summary.get("totalKwh");
        double projectedKwh = daysElapsed > 0 ? (currentKwh / daysElapsed) * totalDays : 0;
        double projectedBill = projectedKwh * rate;
        summary.put("daysElapsed", daysElapsed);
        summary.put("projectedKwh", round(projectedKwh));
        summary.put("projectedBill", round(projectedBill));
        return summary;
    }

    private Map<String, Object> buildSummary(List<UsageLog> logs, double rate, String from, String to) {
        double totalKwh = logs.stream()
            .mapToDouble(log -> log.getKwhConsumed() != null ? log.getKwhConsumed() : 0.0)
            .sum();
        double totalCost = totalKwh * rate;

        // Per-appliance breakdown
        Map<String, Double> applianceKwh = new LinkedHashMap<>();
        Map<String, Double> applianceCost = new LinkedHashMap<>();
        Map<String, String> applianceIcons = new LinkedHashMap<>();

        for (UsageLog log : logs) {
            Appliance appliance = log.getAppliance();
            if (appliance != null) {
                String key = appliance.getName();
                Double kwh = log.getKwhConsumed();
                if (kwh != null) {
                    applianceKwh.merge(key, kwh, (a, b) -> (a != null ? a : 0.0) + (b != null ? b : 0.0));
                    applianceCost.merge(key, log.getCost(rate), (a, b) -> (a != null ? a : 0.0) + (b != null ? b : 0.0));
                }
                applianceIcons.putIfAbsent(key, appliance.getIcon());
            }
        }

        // Sort by cost desc
        List<Map<String, Object>> breakdown = applianceCost.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(e -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", e.getKey());
                item.put("icon", applianceIcons.getOrDefault(e.getKey(), ""));
                Double kwhValue = applianceKwh.get(e.getKey());
                item.put("kwh", kwhValue != null ? round(kwhValue) : 0.0);
                item.put("cost", round(e.getValue() != null ? e.getValue() : 0.0));
                item.put("pct", totalKwh > 0 && kwhValue != null ? round(kwhValue / totalKwh * 100) : 0.0);
                return item;
            }).collect(Collectors.toList());

        // Daily trend for chart
        Map<String, Double> dailyKwh = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        for (UsageLog log : logs) {
            LocalDate usageDate = log.getUsageDate();
            Double kwh = log.getKwhConsumed();
            if (usageDate != null && kwh != null) {
                String day = usageDate.format(fmt);
                dailyKwh.merge(day, kwh, (a, b) -> a + b);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("totalKwh", round(totalKwh));
        result.put("totalCost", round(totalCost));
        result.put("ratePerKwh", rate);
        result.put("breakdown", breakdown);
        result.put("chartLabels", new ArrayList<>(dailyKwh.keySet()));
        result.put("chartKwh", new ArrayList<>(dailyKwh.values()).stream().map(v -> v != null ? round(v) : 0.0).collect(Collectors.toList()));
        result.put("logCount", logs.size());
        return result;
    }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }
}