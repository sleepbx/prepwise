# PrepWise

Live app: https://prepwise-mu-one.vercel.app

PrepWise is a DSA interview analytics platform that turns problem-solving history into a focused preparation plan. It tracks practice, measures readiness, identifies weak areas, prioritizes revision, and recommends the next highest-impact actions for software engineering interviews.

Built for SDE-1 interview preparation across companies like Google, Amazon, Microsoft, Adobe, Meta, Uber, Stripe, and Atlassian.

---

## Why It Stands Out

Most prep trackers count solved problems. PrepWise measures whether the practice is actually improving interview readiness.

| Capability | What It Delivers |
|---|---|
| Interview readiness score | A weighted score based on coverage, accuracy, consistency, revision rate, speed, and difficulty mix |
| Weakness analysis | Topic-level diagnosis with confidence, effort estimate, expected readiness gain, and recommended focus |
| Revision priorities | Spaced repetition queue grouped into Immediate, This Week, and Later |
| Company advisor | Gap analysis against interview patterns for supported companies |
| Daily coach | A concise daily plan based on recent work, streak risk, and highest-ROI tasks |
| Study plan | Week-by-week roadmap from current gaps and target interview date |
| Analytics dashboard | Velocity, pattern radar, difficulty distribution, hint usage, and week-over-week movement |

---

## Live Deployment

| Service | URL |
|---|---|
| Frontend | https://prepwise-mu-one.vercel.app |
| Backend API | https://prepwise-7zvd.onrender.com |

The frontend is deployed on Vercel. API requests under `/api/*` are proxied to the Render backend using `frontend/vercel.json`.

Demo account:

| Email | Password |
|---|---|
| `demo@prepwise.dev` | `demo123` |

---

## Product Architecture

PrepWise separates intelligence from explanation. The backend computes the actual metrics first; language models only turn those metrics into readable guidance.

```text
Problem Attempts
  -> Feature Extraction
  -> Metrics Engine
  -> Rule Engine
  -> Compact Insight DTO
  -> LLM Explanation
  -> React Dashboard
```

This keeps recommendations grounded in deterministic data. If external model providers are unavailable, the app still produces useful rule-based insights.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, Tailwind CSS 4, Recharts, Framer Motion, React Router 7 |
| Backend | Spring Boot 3.5.3, Java 21, Spring Security, JWT, Spring Data JPA |
| Database | SQLite |
| Intelligence | Deterministic Java scoring plus Groq and Gemini explanations |
| Deployment | Vercel frontend, Render Docker backend |
| Build tools | Maven Wrapper, npm |

---

## Core Features

### Readiness Score

A composite score built from practical interview signals:

- Topic coverage
- Accuracy
- Consistency
- Revision quality
- Time efficiency
- Difficulty distribution

### Weakness Analysis

Ranks weak zones by combining performance data with interview importance. A low-coverage high-value topic, such as Graphs or Dynamic Programming, gets prioritized over low-impact gaps.

### Revision System

Uses spaced repetition intervals and decay-weighted scoring to keep old solves from going stale.

### Company Advisor

Compares the user's preparation profile against company-specific topic weights and recommends the best next improvements.

### Daily Coach

Summarizes yesterday, chooses today's highest-ROI task, warns about streak risk, and gives one measurable goal.

---

## Getting Started Locally

### Prerequisites

- Java 21+
- Node.js 18+
- Optional Groq or Gemini API key for model-backed explanations

### Backend

```bash
git clone https://github.com/sleepbx/prepwise.git
cd prepwise

cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

./mvnw spring-boot:run
```

Backend runs at:

```text
http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at:

```text
http://localhost:5173
```

During local development, Vite proxies `/api` to `http://localhost:8080`.

---

## Configuration

Local secrets belong in:

```text
src/main/resources/application-local.properties
```

Example:

```properties
PREPWISE_AI_ENABLED=true

GROQ_API_KEY=your_groq_key_here
GROQ_MODEL=llama-3.1-8b-instant
GROQ_ALLOW_AUTO_INSIGHTS=true

GEMINI_API_KEY=your_gemini_key_here
GEMINI_MODEL=gemini-2.0-flash-001

AI_TOKEN_BUDGET_PER_MINUTE=4000
AI_REQUEST_BUDGET_PER_MINUTE=4
AI_MAX_OUTPUT_TOKENS=160
```

The app falls back to deterministic rule-based insights when provider keys are missing or budgets are exhausted.

---

## API Overview

### Authentication

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create a user account |
| POST | `/api/auth/login` | Login and receive a JWT |

All other protected endpoints require:

```text
Authorization: Bearer <token>
```

### Intelligence

| Method | Path | Description |
|---|---|---|
| GET | `/api/ai/readiness` | Readiness score and breakdown |
| GET | `/api/ai/weakness-analysis` | Topic weakness diagnosis |
| GET | `/api/ai/revision-priorities` | Spaced revision queue |
| GET | `/api/ai/insights` | Analytics insights |
| GET | `/api/ai/daily-coach` | Daily coaching briefing |
| POST | `/api/ai/company-advisor` | Company-specific gap analysis |
| POST | `/api/ai/study-plan` | Interview study plan |
| GET | `/api/ai/companies` | Supported companies |

### Practice And Analytics

| Method | Path | Description |
|---|---|---|
| POST | `/api/problems` | Log a problem attempt |
| GET | `/api/problems/resolve?url=` | Resolve problem metadata from URL |
| GET | `/api/analytics/dashboard` | Dashboard summary |
| GET | `/api/analytics/velocity` | Daily solve velocity |
| GET | `/api/analytics/radar` | Pattern radar data |
| GET | `/api/practice/question-of-day` | Recommended daily problem |
| GET | `/api/practice/review-queue` | Revision queue |

---

## Project Structure

```text
prepwise/
  src/main/java/prepwise/prepwise/
    ai/
      controller/       AI endpoints
      dto/              AI response objects
      intelligence/     Feature extraction and analytics snapshots
      llm/              Groq, Gemini, and prompt orchestration
      service/          Readiness, weakness, study plan, advisor, coaching
      util/             Scoring utilities and company weights
    config/             Seed data and web configuration
    controller/         Auth, problems, analytics, practice, profile
    dto/                Request and response objects
    entity/             User, attempts, streaks, enums
    repository/         Spring Data repositories
    security/           JWT and Spring Security
    service/            Core business logic
  src/main/resources/
    application.properties
    application-local.properties.example
    datasets/
  frontend/
    src/
      api/
      components/
      context/
      pages/
```

---

## Deployment Notes

### Vercel

The frontend is deployed from `frontend/`.

Recommended Vercel settings:

| Setting | Value |
|---|---|
| Framework | Vite |
| Root directory | `frontend` |
| Build command | `npm run build` |
| Output directory | `dist` |

The API rewrite is defined in:

```text
frontend/vercel.json
```

### Render

The backend is deployed as a Docker web service from the `deploy` branch.

Important Render environment variables:

| Variable | Purpose |
|---|---|
| `GROQ_API_KEY` | Groq model access |
| `GEMINI_API_KEY` | Gemini fallback access |
| `JWT_SECRET` | JWT signing secret |
| `PREPWISE_AI_ENABLED` | Enable or disable model-backed explanations |
| `AI_TOKEN_BUDGET_PER_MINUTE` | Free-tier token guardrail |
| `AI_REQUEST_BUDGET_PER_MINUTE` | Free-tier request guardrail |
| `AI_MAX_OUTPUT_TOKENS` | Maximum response length |

SQLite is used for simple deployment. On free Render instances, filesystem data can be ephemeral unless a persistent disk is configured.

---

## Verification

Current production checks:

```text
GET  https://prepwise-mu-one.vercel.app
POST https://prepwise-mu-one.vercel.app/api/auth/login
GET  https://prepwise-7zvd.onrender.com/index.html
```

The Vercel login request routes through Render and returns a JWT for the demo account.

---

## License

MIT
