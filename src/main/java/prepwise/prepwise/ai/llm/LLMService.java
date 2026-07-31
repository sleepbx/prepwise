package prepwise.prepwise.ai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class LLMService {

    private final GeminiClient geminiClient;
    private final GroqClient groqClient;
    private final ObjectMapper objectMapper;
    private final String preferredProvider;
    private final boolean allowGroqForAutoInsights;
    private final int tokenBudgetPerMinute;
    private final int requestBudgetPerMinute;
    private final int maxOutputTokens;
    private final Map<String, CachedAnswer> cache = new ConcurrentHashMap<>();
    private final AtomicInteger usedTokensThisMinute = new AtomicInteger(0);
    private final AtomicInteger usedRequestsThisMinute = new AtomicInteger(0);
    private volatile Instant budgetWindowStart = Instant.now();

    public LLMService(
            GeminiClient geminiClient,
            GroqClient groqClient,
            ObjectMapper objectMapper,
            @Value("${prepwise.ai.provider:${LLM_MODE:groq}}") String preferredProvider,
            @Value("${prepwise.ai.groq.allow-auto-insights:true}") boolean allowGroqForAutoInsights,
            @Value("${prepwise.ai.token-budget-per-minute:4000}") int tokenBudgetPerMinute,
            @Value("${prepwise.ai.request-budget-per-minute:4}") int requestBudgetPerMinute,
            @Value("${prepwise.ai.max-output-tokens:160}") int maxOutputTokens) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
        this.preferredProvider = preferredProvider == null ? "groq" : preferredProvider.toLowerCase(Locale.ROOT);
        this.allowGroqForAutoInsights = allowGroqForAutoInsights;
        this.tokenBudgetPerMinute = Math.max(1000, tokenBudgetPerMinute);
        this.requestBudgetPerMinute = Math.max(1, requestBudgetPerMinute);
        this.maxOutputTokens = Math.max(80, maxOutputTokens);
    }

    // Groq (llama-3.1-8b-instant): fast responses, 14 400 req/day free — used for short/interactive features
    // Gemini (gemini-2.0-flash-001): better reasoning, 1 500 req/day free — used for complex multi-topic analysis
    public String generateStudyPlanExplanation(Map<String, Object> summary) {
        return explain(AiPrompts.studyPlan(toCompactJson(summary)), fallbackStudyPlan(summary), false, "groq");
    }

    public String generateWeaknessExplanation(Map<String, Object> summary) {
        return explain(AiPrompts.weakness(toCompactJson(summary)), fallbackWeakness(summary), true, "gemini");
    }

    public String generateReadinessExplanation(Map<String, Object> summary) {
        return explain(AiPrompts.readiness(toCompactJson(summary)), fallbackReadiness(summary), true, "groq");
    }

    public String generateCompanyAdvice(Map<String, Object> summary) {
        return explain(AiPrompts.companyAdvice(toCompactJson(summary)), fallbackCompany(summary), false, "gemini");
    }

    public String generateAnalyticsInsight(Map<String, Object> summary) {
        return explain(AiPrompts.analyticsInsight(toCompactJson(summary)), fallbackAnalytics(summary), true, "groq");
    }

    public String generateDailyCoachMessage(Map<String, Object> summary) {
        return explain(AiPrompts.dailyCoach(toCompactJson(summary)), fallbackCoach(summary), true, "groq");
    }

    private String explain(String userPrompt, String fallback, boolean automaticInsight, String featureProvider) {
        String cacheKey = (automaticInsight ? "auto:" : "explicit:") + userPrompt;
        CachedAnswer cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.answer();
        }

        int estimatedTokens = estimateTokens(AiPrompts.MENTOR_SYSTEM) + estimateTokens(userPrompt) + maxOutputTokens;
        if (!reserveBudget(estimatedTokens)) {
            log.info("Skipping LLM call to stay within {} TPM / {} RPM budget",
                    tokenBudgetPerMinute, requestBudgetPerMinute);
            return fallback;
        }

        String response = callWithProvider(featureProvider, userPrompt, automaticInsight);
        if (response != null && !response.isBlank()) {
            String answer = response.trim();
            cache.put(cacheKey, new CachedAnswer(answer, Instant.now().plus(Duration.ofMinutes(10))));
            return answer;
        }
        return fallback;
    }

    private String callWithProvider(String provider, String userPrompt, boolean automaticInsight) {
        return switch (provider) {
            case "groq" -> {
                String r = callGroq(userPrompt, automaticInsight);
                yield r != null ? r : callGemini(userPrompt);
            }
            case "gemini" -> {
                String r = callGemini(userPrompt);
                yield r != null ? r : callGroq(userPrompt, automaticInsight);
            }
            default -> {
                // respect global preference if no feature override matched
                if ("groq".equals(preferredProvider)) {
                    String r = callGroq(userPrompt, automaticInsight);
                    yield r != null ? r : callGemini(userPrompt);
                }
                String r = callGemini(userPrompt);
                yield r != null ? r : callGroq(userPrompt, automaticInsight);
            }
        };
    }

    private String callGemini(String userPrompt) {
        return geminiClient.isEnabled() ? geminiClient.generate(AiPrompts.MENTOR_SYSTEM, userPrompt) : null;
    }

    private String callGroq(String userPrompt, boolean automaticInsight) {
        if (automaticInsight && !allowGroqForAutoInsights) {
            return null;
        }
        return groqClient.isEnabled() ? groqClient.generate(AiPrompts.MENTOR_SYSTEM, userPrompt) : null;
    }

    private synchronized boolean reserveBudget(int estimatedTokens) {
        Instant now = Instant.now();
        if (Duration.between(budgetWindowStart, now).toSeconds() >= 60) {
            budgetWindowStart = now;
            usedTokensThisMinute.set(0);
            usedRequestsThisMinute.set(0);
        }
        if (usedRequestsThisMinute.get() >= requestBudgetPerMinute) {
            return false;
        }
        if (usedTokensThisMinute.get() + estimatedTokens > tokenBudgetPerMinute) {
            return false;
        }
        usedRequestsThisMinute.incrementAndGet();
        usedTokensThisMinute.addAndGet(estimatedTokens);
        return true;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return Math.max(1, text.length() / 4);
    }

    private String toCompactJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return data.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private String fallbackStudyPlan(Map<String, Object> s) {
        var topics = (java.util.List<?>) s.getOrDefault("priorityTopics", java.util.List.of());
        int days = Integer.parseInt(s.getOrDefault("daysRemaining", 30).toString());
        int hours = Integer.parseInt(s.getOrDefault("totalHours", 0).toString());
        Object baseline = s.getOrDefault("baselineReadiness", 0);
        String topStr = topics.isEmpty() ? "the highest-gap topics" : topics.stream().limit(3)
                .map(Object::toString).collect(java.util.stream.Collectors.joining(", "));
        return days + " days and " + hours + " hours available. " +
               "Baseline readiness is " + baseline + ", so the plan front-loads " + topStr +
               " because they combine high company weight with low coverage or weak retention signals. " +
               "The schedule moves from concept repair to speed blocks, then mixed mocks and revision, with one secondary topic each week to prevent forgetting. " +
               "Use no-hint first attempts, revision completion, and timed mixed sets as the quality gates.";
    }

    @SuppressWarnings("unchecked")
    private String fallbackWeakness(Map<String, Object> s) {
        String topWeakness = s.getOrDefault("topWeakness", "the top weak topic").toString();
        Object coverage = s.getOrDefault("coverage", "low");
        Object importance = s.getOrDefault("interviewImportance", "high");
        Object avgTime = s.getOrDefault("avgSolveTime", 0);
        return topWeakness + " coverage at " + coverage + "% with " + importance + "% interview importance is your highest-priority gap. " +
               "Average solve time of " + avgTime + "m suggests concept gaps rather than speed issues — study pattern templates before grinding volume. " +
               "Solve 2 medium problems per day in this topic without hints, then revise one older problem within 48 hours to build retention.";
    }

    private String fallbackReadiness(Map<String, Object> s) {
        double score = Double.parseDouble(s.getOrDefault("score", 0).toString());
        String level = s.getOrDefault("level", "ON_TRACK").toString();
        String outcome = s.getOrDefault("expectedOutcome", "Phone Screen Ready").toString();
        String risk = s.getOrDefault("hiringRisk", "MEDIUM").toString();
        return "Readiness score: " + score + " — " + level.replace("_", " ") + ". Expected outcome: " + outcome + ". Hiring risk: " + risk + ". " +
               "Coverage and consistency are the two highest-weight components in the score — both compound over time. " +
               "Prioritize daily active sessions over session length, close Blind 75 gaps systematically, and revise 2–3 high-priority problems per session. " +
               "A 10-point gain in consistency alone typically adds 2.5 readiness points.";
    }

    @SuppressWarnings("unchecked")
    private String fallbackCompany(Map<String, Object> s) {
        String company = s.getOrDefault("company", "this company").toString();
        var topGaps = (java.util.List<Map<String, Object>>) s.getOrDefault("topGaps", java.util.List.of());
        if (topGaps.isEmpty()) {
            return "Focus on your coverage gaps relative to " + company + "'s interview distribution. " +
                   "Drill pattern templates rather than accumulating problem volume. " +
                   "Spaced revision of solved problems improves retention and reduces interview-day memory gaps.";
        }
        var first = topGaps.get(0);
        int gap = Integer.parseInt(first.getOrDefault("gap", 0).toString());
        String topic = first.getOrDefault("topic", "the top gap topic").toString();
        return topic + " has a " + gap + "% coverage gap vs. " + company + "'s expected distribution — this is your highest-ROI improvement. " +
               "Solving 10–15 focused problems in this area is estimated to close the gap within 2 weeks. " +
               "Avoid spending additional time on topics already at or above expected coverage; redirect that effort to gaps. " +
               "Prioritize patterns, not platforms.";
    }

    private String fallbackAnalytics(Map<String, Object> s) {
        int solvedDiff = Integer.parseInt(s.getOrDefault("problemsSolvedDiff", 0).toString());
        int activeDiff = Integer.parseInt(s.getOrDefault("activeDaysDiff", 0).toString());
        Object last7 = s.getOrDefault("last7Problems", 0);
        String direction = solvedDiff > 0 ? "up " + solvedDiff : solvedDiff < 0 ? "down " + Math.abs(solvedDiff) : "flat";
        return "Problem volume " + direction + " vs. last week. Active days changed by " + activeDiff + ". " +
               (solvedDiff < -2 ? "Volume dropped significantly — investigate whether this reflects reduced time or increased difficulty. Protect streak." :
                solvedDiff < 0  ? "Slight volume dip — ensure difficulty is not regressing. Keep revision rate steady." :
                solvedDiff > 3  ? "Strong volume increase (" + last7 + " problems this week). Verify accuracy is holding — volume without retention is low ROI." :
                                  "Volume is stable. Focus on accuracy and revision rate as the next quality lever.") +
               " Hint usage and solve time are equally important signals — track them alongside problem count.";
    }

    @SuppressWarnings("unchecked")
    private String fallbackCoach(Map<String, Object> s) {
        String company = s.getOrDefault("company", "your target company").toString();
        Object streak = s.getOrDefault("currentStreak", 0);
        Object readiness = s.getOrDefault("readinessScore", 0);
        String weakTopic = s.getOrDefault("weakTopic", "the weakest topic").toString();
        int importance = Integer.parseInt(s.getOrDefault("topicImportance", 80).toString());
        return "Streak at " + streak + " days. Readiness: " + readiness + ". " +
               weakTopic + " (" + importance + "% interview importance for " + company + ") is today's highest-ROI focus. " +
               "Solve 2 medium problems in " + weakTopic + " without hints before reviewing new material. " +
               "Complete any overdue revision items first — retention compounds.";
    }

    private record CachedAnswer(String answer, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
