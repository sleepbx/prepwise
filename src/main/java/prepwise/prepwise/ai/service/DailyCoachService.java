package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.DailyCoachRequest;
import prepwise.prepwise.ai.dto.DailyCoachResponse;
import prepwise.prepwise.ai.dto.ReadinessResponse;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.intelligence.UserAnalyticsSnapshot;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.ai.util.CompanyPatternWeights;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyCoachService {

    private final ProblemService problemService;
    private final CurrentUserService currentUserService;
    private final AnalyticsFeatureExtractor extractor;
    private final ReadinessService readinessService;
    private final RevisionPriorityService revisionPriorityService;
    private final LLMService llmService;

    public DailyCoachResponse getBriefing() {
        return getBriefing(new DailyCoachRequest());
    }

    public DailyCoachResponse getBriefing(DailyCoachRequest request) {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        UserAnalyticsSnapshot snapshot = extractor.extract(user, attempts);
        ReadinessResponse readiness = readinessService.calculate();
        String company = normalizeCompany(request.getCompany());
        String userPrompt = request.getPrompt() == null || request.getPrompt().isBlank()
                ? "What should I improve today and why?"
                : request.getPrompt().trim();

        String weakTopic = snapshot.topicMetrics().values().stream()
                .filter(m -> m.solvedCount() > 0)
                .max(Comparator.comparingDouble(m -> m.weaknessIndex()))
                .map(m -> m.label())
                .orElse("Arrays");

        var revisions = revisionPriorityService.getPriorities().stream().limit(3).toList();
        List<String> tasks = new java.util.ArrayList<>();
        tasks.add("Revise: " + (revisions.isEmpty() ? "pick 2 problems from History" : revisions.get(0).getTitle()));
        tasks.add("Solve 2 fresh " + weakTopic + " problems without hints");
        tasks.add("Maintain streak - log at least 1 attempt today");

        String headline = "Focus on " + weakTopic + " for " + company + " today.";
        String impact = estimateImpact(readiness.getScore(), revisions.size());

        Map<String, Object> summary = Map.of(
                "userPrompt", userPrompt,
                "headline", headline,
                "tasks", tasks,
                "company", company,
                "companyExpectations", CompanyPatternWeights.forCompany(company),
                "currentStreak", snapshot.currentStreak(),
                "readinessScore", readiness.getScore(),
                "weakTopic", weakTopic
        );

        return DailyCoachResponse.builder()
                .headline(headline)
                .tasks(tasks)
                .estimatedImpact(impact)
                .message(llmService.generateDailyCoachMessage(summary))
                .build();
    }

    private String normalizeCompany(String company) {
        if (company == null || company.isBlank()) return "Google";
        String trimmed = company.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    private String estimateImpact(double readinessScore, int revisionCount) {
        if (readinessScore < 50) return "+3 to +4 readiness points";
        if (revisionCount > 0) return "+2 readiness points";
        return "+1 readiness point";
    }
}
