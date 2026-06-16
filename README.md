# PrepWise

**DSA Preparation Intelligence Dashboard** — track problems, patterns, sheet progress, streaks, and analytics across LeetCode, Codeforces, GeeksforGeeks, and HackerRank.

## Features

- **JWT Authentication** — register, login, protected routes
- **Quick Capture** — paste a problem URL, auto-detect platform, lookup metadata from CSV datasets
- **Sheet Intelligence** — Striver A2Z, CodeHelp DSA, NeetCode 150, Blind 75 progress
- **Analytics** — velocity charts, pattern radar, difficulty mix, improvement trends, weak zones
- **History** — searchable attempt ledger with filters
- **Profile** — streaks, stats, GitHub-style consistency heatmap
- **Demo Data** — pre-seeded account for instant exploration

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| Backend | Java 21, Spring Boot 3.5, Spring Security, JWT, JPA, H2, Lombok |
| Frontend | React, Vite, Tailwind CSS, React Router, Axios, Recharts, Framer Motion |

## Prerequisites

- **Java 21**
- **Maven** (or use included `./mvnw`)
- **Node.js 18+**

## Quick Start

### 1. Backend

```bash
# From project root
./mvnw spring-boot:run
```

Backend runs at **http://localhost:8080**

H2 Console: http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:prepwise` · User: `sa` · Password: *(empty)*

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at **http://localhost:5173** (proxies `/api` to backend)

### 3. Demo Login

| Email | Password |
|-------|----------|
| `demo@prepwise.dev` | `demo123` |

Or register a new account.

## API Endpoints

### Auth
- `POST /api/auth/register` — `{ name, email, password }`
- `POST /api/auth/login` — `{ email, password }`

### Problems
- `POST /api/problems` — `{ problemUrl, timeSpentMinutes, hintUsed }`
- `GET /api/problems?topic=&pattern=&difficulty=&platform=&search=`

### Analytics
- `GET /api/analytics/dashboard`
- `GET /api/analytics/velocity`
- `GET /api/analytics/radar`
- `GET /api/analytics/difficulty`
- `GET /api/analytics/topics`
- `GET /api/analytics/patterns`
- `GET /api/analytics/time-analysis`
- `GET /api/analytics/hints`
- `GET /api/analytics/improvement`
- `GET /api/analytics/weak-zones`

### Profile
- `GET /api/profile`

## Project Structure

```
prep/
├── src/main/java/prepwise/prepwise/
│   ├── config/          # Demo seeder
│   ├── controller/      # REST controllers
│   ├── dto/             # Request/response DTOs
│   ├── entity/          # JPA entities & enums
│   ├── repository/      # Spring Data repos
│   ├── security/        # JWT & Spring Security
│   └── service/         # Business logic
├── src/main/resources/
│   ├── datasets/        # CSV problem metadata
│   └── application.properties
├── frontend/
│   └── src/
│       ├── api/         # Axios client
│       ├── components/  # UI components
│       ├── context/     # Auth context
│       ├── pages/       # Route pages
│       └── utils/       # Formatters
└── scripts/
    └── generate_datasets.py
```

## Regenerating CSV Datasets

```bash
python scripts/generate_datasets.py
```

## Build for Production

```bash
# Backend
./mvnw clean package -DskipTests

# Frontend
cd frontend && npm run build
```

## Configuration

Key settings in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Backend port |
| `prepwise.jwt.secret` | *(set in file)* | JWT signing key |
| `prepwise.jwt.expiration-ms` | `86400000` | Token TTL (24h) |
| `prepwise.seed.demo` | `true` | Seed demo user on startup |

## License

MIT
