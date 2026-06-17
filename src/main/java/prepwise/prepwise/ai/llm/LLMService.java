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

    public String generateStudyPlanExplanation(Map<String, Object> summary) {
        return explain(AiPrompts.studyPlan(toCompactJson(summary)), fallbackStudyPlan(summary), false);
    }

    public String generateWeaknessExplanation(Map<String, Object> summary) {
        return explain(AiPrompts.weakness(toCompactJson(summary)), fallbackWeakness(summary), true);
    }

    public String generateReadinessExplanation(Map<String, Object> summary) {
        return explain(AiPrompts.readiness(toCompactJson(summary)), fallbackReadiness(summary), true);
    }

    public String generateCompanyAdvice(Map<String, Object> summary) {
        return explain(AiPrompts.companyAdvice(toCompactJson(summary)), fallbackCompany(summary), false);
    }

    public String generateAnalyticsInsight(Map<String, Object> summary) {
        return explain(AiPrompts.analyticsInsight(toCompactJson(summary)), fallbackAnalytics(summary), true);
    }

    public String generateDailyCoachMessage(Map<String, Object> summary) {
        return explain(AiPrompts.dailyCoach(toCompactJson(summary)), fallbackCoach(summary), true);
    }

    private String explain(String userPrompt, String fallback, boolean automaticInsight) {
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

        String response = callPreferredProvider(userPrompt, automaticInsight);
        if (response != null && !response.isBlank()) {
            String answer = response.trim();
            cache.put(cacheKey, new CachedAnswer(answer, Instant.now().plus(Duration.ofMinutes(10))));
            return answer;
        }
        return fallback;
    }

    private String callPreferredProvider(String userPrompt, boolean automaticInsight) {
        if ("groq".equals(preferredProvider)) {
            return callGroq(userPrompt, automaticInsight);
        }
        String gemini = callGemini(userPrompt);
        return gemini != null ? gemini : callGroq(userPrompt, automaticInsight);
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
        var weeks = (java.util.List<Map<String, Object>>) s.getOrDefault("weeks", java.util.List.of());
        var gaps = (java.util.List<String>) s.getOrDefault("priorityTopics", java.util.List.of());
        return "You have " + s.get("daysRemaining") + " days and about " + s.get("totalHours") + " hours available. " +
               "Prioritize " + String.join(", ", gaps) + " based on company weight versus your coverage gaps. " +
               "Follow the " + weeks.size() + "-week structure with " + s.get("hoursPerDay") + "h/day: " +
               "front-load weak areas, then mix revision in the final weeks.";
    }

    @SuppressWarnings("unchecked")
    private String fallbackWeakness(Map<String, Object> s) {
        var weak = (java.util.List<String>) s.getOrDefault("weakTopics", java.util.List.of());
        String topicText = weak.isEmpty() ? "your least practiced topics" : String.join(", ", weak);
        return "Your weakest areas are " + topicText + ". " +
               "Low coverage, slow solves, hint usage, revision gaps, and consistency drive the index. " +
               "Solve 2 medium problems per weak topic this week without hints, then re-attempt one old problem within 48 hours.";
    }

    private String fallbackReadiness(Map<String, Object> s) {
        return "Readiness score is " + s.get("score") + " (" + s.get("level") + "). " +
               "Coverage and consistency are your main levers. " +
               "Maintain your streak, close Blind 75 gaps, and revise 3 high-priority problems before adding new ones.";
    }

    @SuppressWarnings("unchecked")
    private String fallbackCompany(Map<String, Object> s) {
        var areas = (java.util.List<String>) s.getOrDefault("focusAreas", java.util.List.of());
        String areaText = areas.isEmpty() ? "the recommended focus areas" : String.join(" and ", areas);
        return areaText + " align with " + s.get("company") + "'s interview distribution. " +
               "Allocate study time proportionally and drill pattern templates, not random problem volume.";
    }

    private String fallbackAnalytics(Map<String, Object> s) {
        int solvedDiff = Integer.parseInt(s.getOrDefault("problemsSolvedDiff", 0).toString());
        return "Problems solved changed by " + solvedDiff + " versus the previous week. " +
               "Solve time moved " + s.get("avgSolveTimeDiff") + " minutes and active days changed by " +
               s.getOrDefault("activeDaysDiff", 0) + ". " +
               (solvedDiff < 0
                       ? "Volume dropped; protect consistency even with fewer problems."
                       : "Good momentum; keep difficulty balanced with revision.");
    }

    @SuppressWarnings("unchecked")
    private String fallbackCoach(Map<String, Object> s) {
        return (String) s.getOrDefault("headline", "Stay consistent today.") + " " +
               "Streak: " + s.get("currentStreak") + " days. Readiness: " + s.get("readinessScore") + ". " +
               "Complete today's revision queue before picking new problems.";
    }

    private record CachedAnswer(String answer, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
