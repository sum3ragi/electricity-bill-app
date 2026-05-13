# ⚡ Watts Ahead — Household Electricity Bill Tracker

A Spring Boot + MySQL + HTML/CSS/JS web app that lets you track electricity consumption **by appliance**, log daily usage hours, and compute your running bill — broken down per device.

---

## 🛠 Tech Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| Backend    | Java 17, Spring Boot 3.2          |
| Database   | MySQL 8+                          |
| ORM        | Spring Data JPA / Hibernate       |
| Frontend   | HTML5, CSS3, Vanilla JS           |
| Charting   | Chart.js 4                        |
| Templates  | Thymeleaf                         |

---

## 📁 Project Structure

```
electricity-bill-app/
├── pom.xml
└── src/main/
    ├── java/com/electricity/bill/
    │   ├── BillProjectionApplication.java        # Entry point
    │   ├── controller/
    │   │   ├── WebController.java                # Serves HTML page
    │   │   └── ApplianceController.java          # REST API
    │   ├── model/
    │   │   ├── Appliance.java                    # Appliance entity (name, watts, icon)
    │   │   ├── UsageLog.java                     # Daily usage entry (hours, quantity)
    │   │   └── AppSettings.java                  # Rate per kWh, billing cycle, household name
    │   ├── repository/
    │   │   ├── ApplianceRepository.java
    │   │   ├── UsageLogRepository.java
    │   │   └── SettingsRepository.java
    │   └── service/
    │       └── ApplianceService.java             # Business logic, billing, presets seed
    └── resources/
        ├── application.properties
        ├── templates/index.html                  # Main UI
        └── static/
            ├── css/style.css
            └── js/app.js
```

---

## ⚙️ Setup & Run

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8+

### 1. Create MySQL Database

Run this in MySQL Workbench or the MySQL CLI:

```sql
CREATE DATABASE IF NOT EXISTS electricity_db;
CREATE USER IF NOT EXISTS 'root'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON electricity_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/electricity_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

### 3. Build & Run

```bash
cd electricity-bill-app
mvn spring-boot:run
```

### 4. Open the App

Visit: **http://localhost:8080**

> Tables are auto-created on first run. 25 preset appliances are seeded automatically.

---

## 🔌 How It Works

### Step 1 — Appliances
Go to the **Appliances** tab. You'll find 25 built-in presets (aircon, ref, TV, etc.) ready to use. You can also add your own custom appliances with a name, wattage, and icon.

### Step 2 — Log Usage
Go to the **Log Usage** tab. Pick an appliance, enter how many hours it was used today and how many units are running. The app instantly computes the kWh and cost.

### Step 3 — View Your Bill
The **Dashboard** shows:
- Today's running cost
- Current billing cycle total with projected end-of-month bill
- Custom date range summary
- Per-appliance cost breakdown with percentage share
- Daily kWh trend chart

### Step 4 — Settings
Click **Settings** (gear icon) to:
- Set your Meralco rate per kWh (default: ₱12.50)
- Set your billing cycle start day (e.g. day 1 for monthly)
- Set your household name

---

## 🔌 REST API Reference

### Settings
| Method | Endpoint        | Description              |
|--------|-----------------|--------------------------|
| GET    | `/api/settings` | Get current settings     |
| PUT    | `/api/settings` | Update rate / cycle day  |

### Appliances
| Method | Endpoint              | Description           |
|--------|-----------------------|-----------------------|
| GET    | `/api/appliances`     | List all appliances   |
| POST   | `/api/appliances`     | Add custom appliance  |
| PUT    | `/api/appliances/{id}`| Update appliance      |
| DELETE | `/api/appliances/{id}`| Delete appliance      |

### Usage Logs
| Method | Endpoint       | Description                        |
|--------|----------------|------------------------------------|
| GET    | `/api/logs`    | Get logs for a date (`?date=`)     |
| POST   | `/api/logs`    | Add a usage log entry              |
| DELETE | `/api/logs/{id}`| Delete a log entry                |

### Billing Summaries
| Method | Endpoint               | Description                              |
|--------|------------------------|------------------------------------------|
| GET    | `/api/summary/daily`   | Summary for a specific day (`?date=`)    |
| GET    | `/api/summary/range`   | Summary for a date range (`?start=&end=`)|
| GET    | `/api/summary/cycle`   | Current billing cycle + projection       |

### Sample POST — Add Usage Log
```json
{
  "applianceId": 3,
  "usageDate": "2026-05-13",
  "hoursUsed": 8,
  "quantity": 1,
  "notes": "Aircon on all night"
}
```

### Sample POST — Add Custom Appliance
```json
{
  "name": "Exhaust Fan",
  "watts": 25,
  "icon": "💨",
  "category": "Cooling"
}
```

---

## 📊 Bill Computation Formula

```
kWh = (Watts × Hours × Quantity) / 1000
Cost = kWh × Rate per kWh

Example:
  1HP Aircon = 746W
  Used 8 hours, 1 unit
  kWh = (746 × 8 × 1) / 1000 = 5.968 kWh
  Cost = 5.968 × ₱12.50 = ₱74.60/day
```

---

## 🏠 Built-in Preset Appliances

| Icon | Appliance                  | Watts   | Category      |
|------|----------------------------|---------|---------------|
| ❄️   | Air Conditioner (1HP)      | 746 W   | Cooling       |
| ❄️   | Air Conditioner (1.5HP)    | 1119 W  | Cooling       |
| ❄️   | Air Conditioner (2HP)      | 1492 W  | Cooling       |
| 🌀   | Electric Fan               | 60 W    | Cooling       |
| 🌀   | Ceiling Fan                | 75 W    | Cooling       |
| 🧊   | Refrigerator (small)       | 100 W   | Kitchen       |
| 🧊   | Refrigerator (medium)      | 150 W   | Kitchen       |
| 🧊   | Refrigerator (large)       | 200 W   | Kitchen       |
| 🍚   | Rice Cooker                | 700 W   | Kitchen       |
| 📦   | Microwave Oven             | 1200 W  | Kitchen       |
| 🔥   | Electric Stove             | 2000 W  | Kitchen       |
| 🚿   | Water Heater               | 1500 W  | Bathroom      |
| 🫧   | Washing Machine            | 500 W   | Laundry       |
| 🫧   | Clothes Dryer              | 2000 W  | Laundry       |
| 📺   | LED TV (32")               | 50 W    | Entertainment |
| 📺   | LED TV (55")               | 100 W   | Entertainment |
| 💻   | Desktop Computer           | 200 W   | Entertainment |
| 💻   | Laptop                     | 65 W    | Entertainment |
| 🎮   | Game Console               | 150 W   | Entertainment |
| 💡   | LED Bulb (9W)              | 9 W     | Lighting      |
| 💡   | Fluorescent Light          | 40 W    | Lighting      |
| 👔   | Electric Iron              | 1000 W  | Others        |
| 💧   | Water Pump                 | 370 W   | Others        |
| 📷   | CCTV Camera                | 5 W     | Others        |
| 📡   | WiFi Router                | 10 W    | Others        |

---

## 💡 Default Settings

| Setting              | Default     |
|----------------------|-------------|
| Rate per kWh         | ₱12.50      |
| Billing Cycle Start  | Day 1       |
| Household Name       | My Household|

> Change these anytime in the Settings panel inside the app.