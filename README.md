# GlobalExchange - Modern Currency Converter Dashboard

GlobalExchange is a clean, modern, and highly responsive currency converter dashboard. Designed as a college mini-project showcase, it features a lightweight, zero-dependency Java SE backend and a stunning glassmorphism-based web frontend with dynamic theme switching and persistence.

---

## 🌟 Features

### 1. Currency Converter Section
- Enter any amount and select source/target currencies.
- Fast, asynchronous calculations.
- Instant currency swap button with smooth rotation transitions.

### 2. Live Exchange Rates Display
- Shows current exchange rates clearly (e.g., `1 USD = 84.00 INR`).
- Dynamic rate formula badge updates automatically.

### 3. Popular Currency Rates Section
- Instantly showcases rates for popular currencies (USD, INR, EUR, GBP, JPY, AUD, CAD) relative to the selected source currency.
- Alternating row styling and interactive triggers (clicking a row sets it as the target currency).

### 4. Currency Information Panel
- Displays currency codes, official symbols, and full names dynamically as selections change.

### 5. Quick Convert Cards
- Interactive shortcut cards for common currency pairs (USD → INR, INR → USD, EUR → INR, GBP → INR) to trigger conversions in one click.

### 6. Dynamic Theme Switching System
- Switch instantly between **Dark Theme**, **Light Theme**, and **Blue Theme** via custom CSS variables.
- Contrast-adjusted elements ensure high accessibility across all themes.
- Preferences persist automatically across page refreshes using browser `localStorage`.

### 7. Mobile-First Responsive Design
- Responsive layout that auto-aligns header elements on mobile screens.
- Touch-friendly input fields, selectors, and buttons.
- Touch-responsive horizontal table scrolling on extra-small screens.

---

## 🛠️ Tech Stack & Architecture

### Backend
- **Core Technology**: Java SE (Version 22+)
- **Server**: Built-in standard library `com.sun.net.httpserver.HttpServer` (zero external framework overhead, no Spring Boot, no Tomcat).
- **JSON Parsing**: Custom regular expression (`Pattern` & `Matcher`) parser for zero-dependency handling of exchange rate payloads.
- **Port Binding**: Dynamically reads the cloud environment `PORT` variable (with a fallback to `8080`).

### Frontend
- **Structure & Layout**: Semantic HTML5, CSS3 Custom Properties (variables) for theme management.
- **Aesthetics**: Glassmorphism cards (`backdrop-filter`), slate-indigo backdrops, and hover transitions.
- **Logic**: Vanilla JavaScript utilizing the Web Fetch API for asynchronous background updates (no page reloads).

---

## 🚀 Getting Started

### Prerequisites
Make sure you have Java Development Kit (JDK) 22 or higher installed:
```cmd
java -version
```

### Local Execution (Command Line)

1. Open your terminal in the project directory.
2. **Compile the source files**:
   ```cmd
   javac -d bin src/com/globalexchange/service/ExchangeRateService.java src/com/globalexchange/server/ExchangeServer.java
   ```
3. **Run the server**:
   ```cmd
   java -cp bin com.globalexchange.server.ExchangeServer
   ```
4. Open your web browser and navigate to:
   **[http://localhost:8080](http://localhost:8080)**

---

## 🐳 Containerization & Cloud Deployment

GlobalExchange is fully containerized and deployment-ready out of the box using the included `Dockerfile`.

### Build & Run via Docker
```bash
# Build the Docker image
docker build -t globalexchange .

# Run the container mapping port 8080
docker run -p 8080:8080 -e PORT=8080 globalexchange
```

### Deploying Free Online
You can host this project publicly on platforms like **Koyeb** or **Render** directly from GitHub:
1. Push your repository to GitHub.
2. Link your repo to a Web Service on Koyeb or Render.
3. The platform will read the `Dockerfile`, build, compile, and publish your dashboard automatically.

---

## 📁 Directory Structure
```
GlobalExchange/
├── bin/                       # Compiled Java class files (git-ignored)
├── src/
│   └── com/
│       └── globalexchange/
│           ├── server/
│           │   └── ExchangeServer.java       # HTTP Server & API Handlers
│           └── service/
│               └── ExchangeRateService.java  # Fetch rates & Currency info
├── web/
│   ├── index.html             # UI Structure
│   ├── style.css              # Glassmorphic themes & layouts
│   └── app.js                 # AJAX calls & UI logic
├── Dockerfile                 # Cloud container configuration
└── README.md                  # Documentation
```
