# PrepWise — Architecture & Engineering Decisions

This document explains how the system is built, why specific decisions were made, and what the tradeoffs are. Written for your own understanding and interview preparation.

---

## 1. High-Level Architecture

```
Browser (React 19)
       │  JWT in Authorization header
       ▼
Spring Boot 8080
       │
       ├── SecurityConfig (JWT filter on all /api/* except auth)
       │
       ├── Controllers (thin — validate input, delegate, return DTO)
       │        │
       │        ├── AnalyticsController → AnalyticsService
       │        ├── ProblemController   → ProblemService
       │        ├── PracticeController  → PracticeService
       │        └── AiController        → AI Services (7 of them)
       │
       ├── Services (business logic lives here)
       │        │
       │        └── AI Services specifically:
       │             AnalyticsFeatureExtractor → UserAnalyticsSnapshot
       │             → IntelligenceScoring → compact Map → LLMService
       │
       └── Repositories (Spring Data JPA)
                │
                └── H2 in-memory database
```

---

## 2. Data Model

Three entities:

**User** — standard auth fields (name, email, bcrypt-hashed password, role).

**ProblemAttempt** — one row per solve attempt. Critically, it is *not* unique by problem. If you solve Two Sum three times, there are three rows. This is intentional — it enables revision tracking, accuracy trends, and spaced repetition scoring.

```
ProblemAttempt
  user_id      → FK to User
  platform     → LEETCODE | CODEFORCES | GFG | HACKERRANK | ...
  slug         → "two-sum" (URL-extracted, used as identifier)
  title        → "Two Sum"
  difficulty   → EASY | MEDIUM | HARD
  topic        → ARRAY | GRAPH | DP | TREE | ... (12 values)
  pattern      → TWO_POINTER | DFS | BFS | KNAPSACK | ... (20 values)
  timeSpentMinutes
  hintUsed     → boolean
  solvedAt     → timestamp
  lastReviewedAt
```

**Streak** — one row per user. `currentStreak` and `longestStreak` are updated by `StreakService` on every new attempt.

### Why no unique problem table?

A separate `Problem` entity would require maintaining a problem database. Instead, PrepWise loads problem metadata from CSV datasets at startup (`MetadataLoader`) and resolves URL → metadata on the fly. This keeps the database simple and lets metadata be updated without migrations.

**Tradeoff:** You can't query "all problems I haven't solved yet." You can only query your attempt history. This is a deliberate scope decision.

---

## 3. Authentication

**JWT, stateless.** No sessions, no server-side state.

Flow:
1. `POST /api/auth/login` → `AuthService` verifies password with `BCryptPasswordEncoder` → `JwtService.generateToken(user)` → returns token
2. Every subsequent request: `JwtAuthFilter` reads `Authorization: Bearer <token>`, validates signature and expiry, sets `SecurityContextHolder`
3. `CurrentUserService.getCurrentUser()` reads the username from the security context and fetches the User from the database

**JwtService** uses HMAC-SHA256. The secret key is in `application.properties` — in production this should come from an environment variable or secrets manager.

**Tradeoff:** No token revocation. If a token is stolen, it's valid until expiry (24 hours). For a portfolio project this is fine. Production fix: add a token blacklist in Redis or use short-lived access tokens + refresh tokens.

---

## 4. URL-based Problem Resolution

When a user pastes a LeetCode/GFG/Codeforces URL:

```
POST /api/problems/resolve?url=https://leetcode.com/problems/two-sum/
         ↓
   UrlParserService.parse(url)
         → detects platform (LEETCODE)
         → extracts slug ("two-sum")
         ↓
   MetadataLoader.resolve(platform, slug)
         → looks up in loaded CSV maps
         → returns ProblemMetadata (title, difficulty, topic, pattern)
         ↓
   Returns ProblemResolveResponse to frontend
```

The frontend pre-fills the log form from this. The user can override any field before logging.

**MetadataLoader** reads 8 CSV files at startup and builds lookup maps keyed by `platform:slug`. Files: `leetcode.csv`, `blind_75.csv`, `neetcode_150.csv`, `striver_a2z.csv`, `codehelp_dsa.csv`, `gfg.csv`, `codeforces.csv`, `hackerrank.csv`.

**Tradeoff:** CSVs are static. New problems added to LeetCode after the CSV was generated won't have metadata. If a problem isn't found, platform and slug are still captured, but topic/pattern default to UNKNOWN. The user can manually set them.

---

## 5. The AI Layer in Detail

### Why "analytics-first, LLM-second"?

LLMs hallucinate statistics. If you ask Gemini "how well have I prepared for graphs based on my history?", it makes up numbers. Worse, you can't trust the output to be consistent.

PrepWise's approach: **compute everything deterministically in Java, then use the LLM only to write a readable sentence about the result.**

The LLM prompt looks like this:
```
Data: {"topWeakness":"Graph","coverage":18,"accuracy":65,"avgSolveTime":52,"interviewImportance":80}
```
Not: "Here are my last 300 solved problems, what do you think?"

This keeps prompts under 100 tokens, responses under 160 tokens, and makes the LLM's job simple enough that even a small free model (llama-3.1-8b-instant) does it well.

---

### AnalyticsFeatureExtractor

Takes `User` + `List<ProblemAttempt>` → produces `UserAnalyticsSnapshot`.

`UserAnalyticsSnapshot` is a Java record:
```java
record UserAnalyticsSnapshot(
    int totalAttempts,
    int uniqueProblems,       // deduplicated by platform:slug
    double overallHintRate,
    double averageSolveTime,
    double revisionCompletionRate,  // % of unique problems solved more than once
    double consistencyScore,        // active days in last 30 / 30
    int currentStreak,
    Map<Topic, TopicMetrics> topicMetrics
)
```

`TopicMetrics` is another record with per-topic coverage, accuracy, avg time, hint rate, revision rate, consistency, and weakness index.

**Coverage** is computed as `min(100, (uniqueSolvedInTopic / 12) * 100)`. The benchmark of 12 is the assumed minimum for reasonable topic coverage. This is a magic number — it could be made configurable.

**Consistency** is `activeDaysInLast30 / 30 * 100`. Simple but effective — it rewards daily practice regardless of volume.

**RevisionRate** is `problemsSolvedMoreThanOnce / totalUniqueProblems * 100`.

---

### IntelligenceScoring

Pure utility class with static methods. No dependencies. All deterministic.

**Readiness score formula:**
```
score = 0.35 × coverage
      + 0.25 × consistency
      + 0.20 × revision
      + 0.15 × accuracy
      + 0.05 × speed
```

Weights reflect what actually moves interview outcomes:
- Coverage is the most important — you need breadth before depth
- Consistency compounds over time — daily practice is worth more than weekend sprints
- Revision is underrated — problems forgotten before interviews don't count
- Accuracy (hint-free rate) matters but is less predictive than coverage
- Speed matters least at the preparation stage — it comes with practice

**Weakness index formula:**
```
weaknessIndex = 0.30 × lowCoverage
              + 0.20 × highSolveTime
              + 0.20 × hintUsage
              + 0.15 × poorRevision
              + 0.15 × inconsistency
```

Each input is pre-normalized to 0–100 range. Higher = worse. Classification: STRONG (≤30), MODERATE (≤60), WEAK (>60).

**Revision priority formula:**
```
priority = 0.25 × timeDifficulty
         + 0.25 × hintDependence
         + 0.25 × revisionDeficit
         + 0.25 × recencyFactor
```

Equal weights because all four factors are genuinely important signals of whether you'll forget a problem.

---

### LLMService

Manages both providers, the token/request budget, and the 10-minute response cache.

**Budget window:** resets every 60 seconds. If `usedTokensThisMinute + estimatedTokens > budget` or `usedRequests >= requestBudget`, the LLM call is skipped and the rule-based fallback is used.

**Token estimation:** `text.length() / 4` — a rough approximation (1 token ≈ 4 characters). Fast, no dependencies.

**Cache key:** the full prompt string. This means identical analytics snapshots (same user data) return cached results for 10 minutes. If data changes (new attempt logged), the prompt changes, cache misses, and a fresh LLM call is made.

**Fallback messages** are in `LLMService` private methods — they receive the same `Map<String, Object>` summary and produce specific, data-referenced text (not generic platitudes). This means the application is useful even with no API keys.

**Tradeoff — cache by prompt:** if two users happen to have identical metric summaries, they could get each other's cached response. In practice this is extremely unlikely since prompts include user-specific numbers, but it's worth knowing. A production fix would prefix the cache key with the user ID.

---

### CompanyPatternWeights

A static map of company → topic distribution weights (summing to 100). These weights represent the approximate topic frequency in each company's DSA interview rounds, based on community aggregated data.

```
GOOGLE:    Graphs 25%, DP 20%, Trees 20%, Arrays 15%, Greedy 10%, Others 10%
AMAZON:    Heap 20%, TopK 20%, Greedy 15%, Trees 15%, Graphs 15%, Others 15%
MICROSOFT: Arrays 20%, Strings 20%, Trees 20%, DP 15%, Graphs 15%, Others 10%
META:      Arrays 25%, Graphs 20%, DP 20%, Strings 15%, Trees 10%, Others 10%
UBER:      Graphs 30%, BinarySearch 20%, Arrays 20%, Greedy 15%, DP 10%, Others 5%
STRIPE:    Arrays 25%, Strings 25%, DP 15%, Greedy 15%, Trees 10%, Others 10%
```

**Tradeoff:** These weights are approximate and become stale as companies change their interview style. They encode a point-in-time understanding of what topics come up. The value is in the relative ordering (Uber values graphs more than strings) rather than exact percentages.

---

### The 7 AI Services

| Service | Key computation | LLM role |
|---|---|---|
| `ReadinessService` | readinessScore formula, hiringRisk, expectedOutcome | Explains strengths, risks, top 3 actions |
| `WeaknessAnalysisService` | weaknessIndex per topic, importance weights, trend | Explains top weakness with specific metrics |
| `CompanyAdvisorService` | topicCoverage vs. company weights → gaps, ROI ranking | Explains top 2 gaps and recommended order |
| `RevisionPriorityService` | revisionPriority formula per unique problem → sorted list | Not called (no LLM for revision list) |
| `StudyPlanService` | gap analysis → prioritized topics → week plans | Explains why these topics were prioritized |
| `AnalyticsInsightService` | week-over-week diffs from AnalyticsService | Interprets trends, flags regressions |
| `DailyCoachService` | weakest topic + pending revisions + streak | Today's briefing referencing all metrics |

All services follow the same pattern:
1. Load user + attempts
2. Call `AnalyticsFeatureExtractor.extract()` to get snapshot
3. Do service-specific computation
4. Build compact `Map<String, Object>` summary (never raw attempt history)
5. Call one `llmService.generateXxx(summary)` method
6. Assemble and return the response DTO

---

## 6. Frontend Architecture

### State management

No Redux, no Zustand. Local `useState` per page. This works because:
- Each page is independently loaded
- Data is not shared between pages (each page fetches its own data)
- The auth token is the only global state — handled by `AuthContext`

For a larger app this would become painful. But for a portfolio project with 6 pages, it's the right call — less code, easier to read.

### API calls

`api/axios.js` exports a pre-configured Axios instance with:
- `baseURL: http://localhost:8080/api`
- Request interceptor that reads the JWT from `localStorage` and adds `Authorization: Bearer <token>`
- No response interceptor — 401s aren't auto-handled (logout on 401 would be a nice addition)

### AI page structure

The AI page uses a tab pattern to avoid overwhelming the user with all 7 AI features at once. Five tabs: Overview, Weakness Analysis, Revision Priorities, Company Advisor, Study Plan.

On mount, Overview tab data (readiness, coach, weaknesses, revisions, insights) loads in parallel with `Promise.allSettled`. Partial failures don't crash the page — individual sections show empty states.

Company Advisor and Study Plan are user-triggered (require a form submission) to avoid burning free API quota on every page load.

### Recharts usage

`RadialBarChart` for the readiness gauge. `BarChart` (horizontal) for weakness index. Recharts is not the most lightweight library (~200KB gzipped) but it covers all needed chart types with a reasonable API.

---

## 7. Known Limitations and Production Gaps

### H2 in-memory database

All data is lost on server restart. This is appropriate for development/demo but would need to be replaced with PostgreSQL (or MySQL) for production.

Migration path:
1. Change `spring.datasource.*` to point to Postgres
2. Change `spring.jpa.hibernate.ddl-auto=create-drop` to `validate` (and use Flyway/Liquibase for schema migrations)
3. No application code changes required — JPA abstracts the dialect

### No token revocation

JWT tokens are valid until expiry (24h). No blacklist. For production: either shorten expiry to 15 minutes + implement refresh tokens, or maintain a Redis-based token blacklist.

### Single-user session assumption

`LLMService` caches by prompt content without a user ID prefix. Two users with identical metric values (very unlikely) could share a cached response. Fix: prefix cache key with user ID.

### No input validation on AI endpoints

`/api/ai/company-advisor` accepts any string for `company`. If the company isn't in `CompanyPatternWeights`, it falls back to Google's weights silently. This should return a 400 with the list of supported companies, or the frontend should be constrained to the supported list (it is, but the backend doesn't enforce it).

### Coverage benchmark is hardcoded

`COVERAGE_BENCHMARK = 12` in `AnalyticsFeatureExtractor`. This means 12 unique problems = 100% topic coverage. This is arbitrary and probably too low for rare topics (Heap) and too aggressive for common ones (Arrays). A better approach would use a per-topic benchmark.

### No persistence for AI responses

AI insights are computed on demand. There's no historical record of your readiness score over time, so you can't see whether you improved month over month. A `ReadinessHistory` table with daily snapshots would enable trend charts.

### Problem metadata is static CSVs

If LeetCode adds new problems or changes slugs, the CSVs go stale. A better architecture would have a problem metadata service that can be updated independently (e.g., a separate microservice or periodic job that refreshes from LeetCode's API).

### No tests

The most significant gap. `IntelligenceScoring` and `AnalyticsFeatureExtractor` are pure functions — they would be trivial to unit test and would provide real confidence that the scoring formulas work correctly at the edges (zero attempts, all hints used, perfect score scenarios).

---

## 8. Things You Can Say in Interviews

**On the AI design:**
> "The LLM receives a compact JSON summary of pre-computed metrics — roughly 50 tokens — not raw problem history. This keeps token usage within free-tier limits, eliminates hallucinated statistics, and ensures the application is fully functional even when both AI providers are unavailable. The LLM's only job is readability, not intelligence."

**On the fallback chain:**
> "There are three layers: Groq as primary, Gemini as fallback, and deterministic rule-based insights as the final fallback. The rule-based messages are written to reference the same specific metrics the LLM would have used, so users don't notice a quality drop."

**On the readiness score:**
> "The formula weights coverage at 35%, consistency at 25%, revision at 20%, accuracy at 15%, and speed at 5%. These reflect what actually predicts interview performance: breadth matters more than depth, daily practice compounds, and revision prevents forgetting. I can walk through how changing any weight affects the score for edge cases."

**On the database choice:**
> "H2 in-memory for the development and demo environment — zero setup, restarts clean. The JPA layer means switching to PostgreSQL for production requires only a config change, no code changes."

**On the weakness index:**
> "It's a weighted sum of five normalized signals: low coverage, slow solve time, hint dependency, poor revision rate, and inconsistency. Each is pre-scaled to 0–100. Low coverage is weighted highest at 30% because unsolved topics are a harder blocker than slow solves for topics you know."
