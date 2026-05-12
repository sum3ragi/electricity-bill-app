# ⚡ Watts Ahead — Electricity Bill Projector

A Spring Boot + MySQL + HTML/CSS/JS web application for tracking electricity usage and projecting future bills using linear regression.

---

## 🛠 Tech Stack

| Layer     | Technology                     |
|-----------|-------------------------------|
| Backend   | Java 17, Spring Boot 3.2       |
| Database  | MySQL 8+                       |
| ORM       | Spring Data JPA / Hibernate    |
| Frontend  | HTML5, CSS3, Vanilla JS        |
| Charting  | Chart.js 4                     |
| Templates | Thymeleaf                      |

---

## 📁 Project Structure

```
electricity-bill-app/
├── pom.xml
└── src/main/
    ├── java/com/electricity/bill/
    │   ├── BillProjectionApplication.java       # Main entry point
    │   ├── controller/
    │   │   ├── WebController.java               # Serves HTML page
    │   │   └── ElectricityController.java       # REST API
    │   ├── model/
    │   │   ├── ElectricityReading.java          # JPA Entity
    │   │   └── BillProjection.java              # Projection DTO
    │   ├── repository/
    │   │   └── ElectricityReadingRepository.java
    │   └── service/
    │       └── ElectricityService.java          # Business logic + Linear Regression
    └── resources/
        ├── application.properties
        ├── templates/index.html                 # Main UI (Thymeleaf)
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
```sql
CREATE DATABASE electricity_db;
CREATE USER 'your_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON electricity_db.* TO 'your_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configure Database Connection
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/electricity_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=your_user
spring.datasource.password=your_password
```

### 3. Build & Run
```bash
cd electricity-bill-app
mvn clean package
mvn spring-boot:run
```

### 4. Open the App
Visit: **http://localhost:8080**

---

## 🔌 REST API Reference

| Method | Endpoint               | Description                      |
|--------|------------------------|----------------------------------|
| GET    | `/api/readings`        | Get all meter readings           |
| POST   | `/api/readings`        | Add a new reading                |
| PUT    | `/api/readings/{id}`   | Update a reading                 |
| DELETE | `/api/readings/{id}`   | Delete a reading                 |
| GET    | `/api/project?months=6`| Project bills for N months       |
| GET    | `/api/stats`           | Get dashboard statistics         |

### Sample POST Body
```json
{
  "readingDate": "2025-05-01",
  "kwhUsage": 280.5,
  "ratePerKwh": 12.00,
  "fixedCharges": 150.00,
  "taxesPercent": 12.0,
  "notes": "Summer month, AC running"
}
```

---

## 📊 How Projection Works

The app uses **simple linear regression** on your historical kWh usage data to project future consumption. The formula is:

```
projected_kwh = intercept + (slope × month_index)
total_bill = (projected_kwh × rate + fixed_charges) × (1 + tax%)
```

- At least **2 readings** are needed for projection
- More historical data = more accurate projections
- The latest reading's rate/charges are used for future projections

---

## 💡 Default Rate Settings (Philippine context)

| Parameter     | Default Value | Notes                  |
|---------------|--------------|------------------------|
| Rate/kWh      | ₱12.00       | Meralco average rate   |
| Fixed Charges | ₱150.00      | Distribution charges   |
| VAT           | 12%          | Philippine VAT         |

You can customize these per reading entry.
