# PrepWise — DSA Interview Analytics Platform

A data-driven preparation analytics platform for software engineering interviews. PrepWise tracks your problem-solving history and surfaces intelligence about your readiness, weak spots, and highest-ROI next actions — backed by deterministic metrics, not guesswork.

> Built as a portfolio project targeting SDE-1 roles at Google, Amazon, Microsoft, Adobe, Meta, Uber, Stripe, and Atlassian.

---

## What It Does

Most DSA prep tools tell you to "practice more." PrepWise tells you *exactly* what to practice, *why*, and *what it's worth in readiness points*.

| Feature | What You Get |
|---|---|
| **Interview Readiness Score** | Weighted composite of coverage, consistency, revision rate, accuracy, and speed — computed in Java, not by an LLM |
| **Weakness Analysis** | Per-topic breakdown with interview importance weights (e.g. Graphs = 80%), confidence level, estimated effort, and expected readiness gain |
| **Revision Priorities** | Spaced repetition queue sorted by a decay-weighted formula, bucketed into Immediate / This Week / Later |
| **Company Advisor** | Coverage gap analysis vs. topic distributions for 8 companies, with high-ROI improvement ranking |
| **Daily Coach** | Yesterday's summary, today's highest-ROI task, streak risk warning, one measurable goal |
| **Study Plan** | Week-by-week roadmap from your coverage gaps and interview date, with daily tasks and expected gains per week |
| **Analytics** | Velocity trends, pattern radar, difficulty mix, hint analysis, week-over-week comparisons |

---

## AI Philosophy

The LLM is **not** responsible for intelligence. The backend is.

```
Attempts
  ↓
Feature Extraction  (Java — coverage, accuracy, solve time, revision rate, consistency)
  ↓
Metrics Engine      (Java — readiness score, weakness index, revision priority)
  ↓
Rule Engine         (Java — classifications, recommendations, gap analysis)
  ↓
Compact DTO         (~50 tokens of structured metrics, not raw history)
  ↓
LLM Explanation     (Groq / Gemini — turns numbers into readable prose)
  ↓
Frontend
```

All statistics and recommendations are computed deterministically in Java first. The LLM receives a compact summary and turns it into readable prose. If both providers are unavailable or the free-tier budget is exhausted, rule-based insights still deliver real value.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5.3, Java 21, Spring Security (JWT), JPA, SQLite, Lombok |
| AI | Groq `llama-3.1-8b-instant` (primary) + Google Gemini `gemini-2.0-flash-001` (fallback) |
| Frontend | React 19, Vite, Tailwind CSS 4, Recharts, Framer Motion, React Router 7 |
| Build | Maven Wrapper, npm |

---

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- A free [Groq API key](https://console.groq.com) — optional, rule-based fallback works without it

### 1. Backend

```bash
git clone https://github.com/your-username/prepwise.git
cd prepwise

# Set up local config (gitignored)
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# Add your GROQ_API_KEY to application-local.properties, then:
./mvnw spring-boot:run
# → http://localhost:8080
```

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

### 3. Demo Account

A demo account with realistic seed data is created automatically on startup.

| Email | Password |
|---|---|
| `demo@prepwise.dev` | `demo123` |

---

## AI Provider Configuration

`src/main/resources/application-local.properties`:

```properties
# Provider: groq (default) or gemini
LLM_MODE=groq
GROQ_API_KEY=your_key_here

# Switch to Gemini as primary, Groq as fallback
# LLM_MODE=gemini
# GEMINI_API_KEY=your_key_here

# Free-tier guardrails — deterministic fallback kicks in when reached
AI_TOKEN_BUDGET_PER_MINUTE=4000
AI_REQUEST_BUDGET_PER_MINUTE=4
AI_MAX_OUTPUT_TOKENS=160
```

**Fallback chain:** Groq → Gemini → Rule-based insights. The application never fails because a provider is unavailable.

---

## Supported Companies

Google · Amazon · Microsoft · Adobe · Meta · Uber · Stripe · Atlassian

Each company has a weighted topic distribution used by the Company Advisor and Study Plan.

---

## API Reference

### Authentication

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Login, returns JWT |

All other endpoints require `Authorization: Bearer <token>`.

### AI

| Method | Path | Description |
|---|---|---|
| GET | `/api/ai/readiness` | Interview readiness score with breakdown |
| GET | `/api/ai/weakness-analysis` | Per-topic weakness index and recommendations |
| GET | `/api/ai/revision-priorities` | Spaced revision queue |
| GET | `/api/ai/insights` | Week-over-week analytics |
| GET | `/api/ai/daily-coach` | Daily coaching briefing |
| POST | `/api/ai/company-advisor` | Gap analysis for a target company |
| POST | `/api/ai/study-plan` | Generate weekly study plan |
| GET | `/api/ai/companies` | List of supported companies |

### Core

| Method | Path | Description |
|---|---|---|
| POST | `/api/problems` | Log a problem attempt |
| GET | `/api/problems/resolve?url=` | Auto-detect problem metadata from URL |
| GET | `/api/analytics/dashboard` | Dashboard stats |
| GET | `/api/analytics/velocity` | Daily solve velocity |
| GET | `/api/analytics/radar` | Pattern radar data |
| GET | `/api/practice/question-of-day` | Today's recommended problem |
| GET | `/api/practice/review-queue` | Revision queue |

---

## Project Structure

```
prepwise/
├── src/main/java/prepwise/prepwise/
│   ├── entity/           # User, ProblemAttempt, Streak, enums (Topic, Pattern, Difficulty, Platform)
│   ├── controller/       # Auth, Problems, Analytics, Practice, Achievements, Profile
│   ├── service/          # Business logic, StreakService, MetadataLoader, UrlParserService
│   ├── repository/       # Spring Data JPA repositories
│   ├── dto/              # Request/response objects
│   ├── security/         # JwtService, JwtAuthFilter, SecurityConfig
│   ├── config/           # DemoDataSeeder
│   └── ai/
│       ├── intelligence/ # AnalyticsFeatureExtractor, UserAnalyticsSnapshot, TopicMetrics
│       ├── llm/          # GroqClient, GeminiClient, LLMService, AiPrompts
│       ├── service/      # ReadinessService, WeaknessAnalysisService, CompanyAdvisorService, etc.
│       ├── dto/          # AI-specific response objects
│       ├── util/         # IntelligenceScoring, CompanyPatternWeights
│       └── controller/   # AiController, AiExceptionHandler
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.properties.example
│   └── datasets/         # leetcode.csv, blind_75.csv, neetcode_150.csv, striver_a2z.csv, etc.
└── frontend/
    └── src/
        ├── pages/        # Dashboard, Analytics, Practice, AI, History, Profile
        ├── components/   # Layout, Sidebar, CountUp
        ├── context/      # AuthContext
        └── api/          # Axios client with JWT interceptor
```

---

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `prepwise.ai.enabled` | `true` | Enable LLM explanations |
| `prepwise.ai.provider` | `groq` | Primary provider (`groq` or `gemini`) |
| `prepwise.ai.token-budget-per-minute` | `4000` | Free-tier token guardrail |
| `prepwise.ai.request-budget-per-minute` | `4` | Free-tier request guardrail |
| `prepwise.ai.max-output-tokens` | `160` | Max tokens per LLM response |
| `prepwise.seed.demo` | `true` | Seed demo account on startup |

---

## Deployment

The `deploy` branch is ready for one-click deployment on [Render](https://render.com) (free tier, Docker runtime):

1. Push the `deploy` branch to GitHub
2. In Render → New Web Service → connect your GitHub repo → select `deploy` branch
3. Render auto-detects the `Dockerfile` and `render.yaml`
4. Set these environment variables in Render's dashboard:
   - `GROQ_API_KEY` — from [console.groq.com](https://console.groq.com)
   - `GEMINI_API_KEY` — from [aistudio.google.com](https://aistudio.google.com)
   - `JWT_SECRET` — Render auto-generates this (defined in render.yaml)

That's it. Frontend and backend are served from the same URL.

**Note on cold starts:** Render free tier spins down after 15 minutes of inactivity. First request after sleep takes ~30 seconds. Acceptable for a portfolio demo; upgrade to the $7/month plan to eliminate this.

---

## License

MIT
