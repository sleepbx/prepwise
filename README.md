# PrepWise

**DSA Preparation Intelligence Dashboard** — track problems, patterns, sheet progress, streaks, and analytics across LeetCode, Codeforces, GeeksforGeeks, and HackerRank.

## Features

### V1 — Core

- **JWT Authentication** — register, login, protected routes
- **Quick Capture** — paste a problem URL, auto-detect platform, lookup metadata from CSV datasets
- **Sheet Intelligence** — Striver A2Z, CodeHelp DSA, NeetCode 150, Blind 75 progress
- **Analytics** — velocity charts, pattern radar, difficulty mix, improvement trends, weak zones
- **History** — searchable attempt ledger with filters
- **Profile** — streaks, stats, GitHub-style consistency heatmap
- **Demo Data** — pre-seeded account for instant exploration

### V2 — AI Intelligence Layer

Analytics-first preparation intelligence with optional LLM explanations (not a chatbot).

- **Study Plan Generator** — company-weighted gap analysis → weekly roadmap
- **Weakness Analyzer** — per-topic weakness index with classifications
- **Interview Readiness Score** — weighted composite (coverage, consistency, revision, accuracy, speed)
- **Revision Priorities** — dynamic spaced-repetition scoring
- **Company Advisor** — Google / Amazon / Microsoft / Adobe pattern weights
- **Analytics Insights** — week-over-week trend narration
- **Daily Coach** — streak + readiness briefing

Architecture: `Attempts → Feature Extraction → Intelligence Engine → LLM Explanation`

## Tech Stack


| Layer    | Technologies                                                               |
| -------- | -------------------------------------------------------------------------- |
| Backend  | Java 21, Spring Boot 3.5, Spring Security, JWT, JPA, H2, Lombok |
| Frontend | React, Vite, Tailwind CSS, React Router, Axios, Recharts, Framer Motion    |


## Prerequisites

- **Java 21**
- **Maven** (or use included `./mvnw`)
- **Node.js 18+**
- **Groq API key** (optional — rule-based fallback works without it)

## Quick Start

### 1. Backend

```bash
./mvnw spring-boot:run
```

Backend runs at **[http://localhost:8080](http://localhost:8080)**

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at **[http://localhost:5173](http://localhost:5173)**

### 3. Demo Login


| Email               | Password  |
| ------------------- | --------- |
| `demo@prepwise.dev` | `demo123` |


### 4. Enable LLM Explanations (optional)

Copy `src/main/resources/application-local.properties.example` to `application-local.properties` (gitignored). It is auto-loaded on startup.

```properties
prepwise.ai.enabled=true
prepwise.ai.provider=groq
prepwise.ai.groq.allow-auto-insights=true
prepwise.ai.token-budget-per-minute=4000
prepwise.ai.request-budget-per-minute=4
prepwise.ai.max-output-tokens=160
prepwise.ai.groq.api-key=YOUR_GROQ_API_KEY
prepwise.ai.groq.model=llama-3.1-8b-instant
```

PrepWise uses Groq only by default. If the per-minute request or token budget is reached, deterministic mentor-style fallback text is used instead of making another LLM call.

## AI API Endpoints


| Method | Endpoint                      | Description                    |
| ------ | ----------------------------- | ------------------------------ |
| POST   | `/api/ai/study-plan`          | Personalized weekly study plan |
| GET    | `/api/ai/weakness-analysis`   | Per-topic weakness index       |
| GET    | `/api/ai/readiness`           | Interview readiness score      |
| GET    | `/api/ai/revision-priorities` | Spaced revision queue          |
| POST   | `/api/ai/company-advisor`     | Company-specific focus areas   |
| POST   | `/api/ai/analytics-insights`  | Week-over-week insight         |
| GET    | `/api/ai/daily-coach`         | Daily coaching briefing        |


### Study Plan Request Example

```json
{
  "targetCompany": "Google",
  "interviewDate": "2026-09-01",
  "hoursPerDay": 3,
  "experienceLevel": "INTERMEDIATE"
}
```

## Core API Endpoints

### Auth

- `POST /api/auth/register` · `POST /api/auth/login`

### Problems

- `POST /api/problems` · `GET /api/problems` · `GET /api/problems/resolve?url=`

### Analytics

- `GET /api/analytics/dashboard` · `/velocity` · `/radar` · `/improvement` · etc.

### Profile

- `GET /api/profile`

## Project Structure

```
prep/
├── src/main/java/prepwise/prepwise/
│   ├── ai/                  # V2 intelligence layer
│   │   ├── controller/
│   │   ├── service/
│   │   ├── dto/
│   │   ├── llm/
│   │   ├── intelligence/
│   │   └── util/
│   ├── config/
│   ├── controller/
│   ├── service/
│   └── ...
├── frontend/src/pages/AI.jsx
└── scripts/generate_datasets.py
```

## Configuration


| Property              | Default | Description                       |
| --------------------- | ------- | --------------------------------- |
| `prepwise.ai.enabled` | `true` | Enable LLM explanations           |
| `prepwise.ai.provider`| `groq`  | Groq-only by default              |
| `prepwise.ai.groq.allow-auto-insights` | `true` | Let Groq power AI Coach auto insights |
| `prepwise.ai.token-budget-per-minute` | `4000` | Conservative free-tier token guardrail |
| `prepwise.ai.request-budget-per-minute` | `4` | Conservative free-tier request guardrail |
| `prepwise.ai.max-output-tokens` | `160` | Caps each Groq response size |
| `prepwise.ai.groq.api-key` | — | Groq API key |
| `prepwise.seed.demo`  | `true`  | Seed demo user on startup         |


## License

MIT
