package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.ReadinessResponse;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.intelligence.UserAnalyticsSnapshot;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.ai.util.IntelligenceScoring;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadinessService {

    private final ProblemService problemService;
    private final CurrentUserService currentUserService;
    private final AnalyticsFeatureExtractor extractor;
    private final LLMService llmService;

    public ReadinessResponse calculate() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        UserAnalyticsSnapshot snapshot = extractor.extract(user, attempts);

        int blindSolved = extractor.blind75Solved(user, attempts);
        double coverage = Math.min(100, (blindSolved * 100.0) / 75);
        double consistency = Math.min(100, snapshot.currentStreak() * 8.0);
        double revision = snapshot.revisionCompletionRate();
        double accuracy = 100 - snapshot.overallHintRate();
        double speed = Math.max(0, 100 - snapshot.averageSolveTime());

        double score = IntelligenceScoring.readinessScore(coverage, consistency, revision, accuracy, speed);
        String level = IntelligenceScoring.readinessLevel(score);

        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("coverage", IntelligenceScoring.round1(coverage));
        breakdown.put("consistency", IntelligenceScoring.round1(consistency));
        breakdown.put("revision", IntelligenceScoring.round1(revision));
        breakdown.put("accuracy", IntelligenceScoring.round1(accuracy));
        breakdown.put("speed", IntelligenceScoring.round1(speed));

        Map<String, Object> summary = Map.of(
                "score", IntelligenceScoring.round1(score),
                "level", level,
                "breakdown", breakdown,
                "blind75Solved", blindSolved,
                "currentStreak", snapshot.currentStreak()
        );

        List<String> keyStrengths = keyStrengths(breakdown);
        List<String> improvementAreas = improvementAreas(breakdown);

        return ReadinessResponse.builder()
                .score(IntelligenceScoring.round1(score))
                .level(level)
                .breakdown(breakdown)
                .keyStrengths(keyStrengths)
                .improvementAreas(improvementAreas)
                .explanation(llmService.generateReadinessExplanation(summary))
                .build();
    }

    private List<String> keyStrengths(Map<String, Double> breakdown) {
        List<String> strengths = new ArrayList<>();
        breakdown.entrySet().stream()
                .filter(e -> e.getValue() >= 70)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> strengths.add(label(e.getKey()) + " is in a healthy range"));
        if (strengths.isEmpty()) {
            strengths.add("You have a measurable baseline to improve from");
        }
        return strengths;
    }

    private List<String> improvementAreas(Map<String, Double> breakdown) {
        List<String> areas = new ArrayList<>();
        breakdown.entrySet().stream()
                .filter(e -> e.getValue() < 70)
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .forEach(e -> areas.add("Improve " + label(e.getKey()).toLowerCase()));
        if (areas.isEmpty()) {
            areas.add("Maintain revision discipline and interview simulation practice");
        }
        return areas;
    }

    private String label(String metric) {
        return switch (metric) {
            case "coverage" -> "Coverage";
            case "consistency" -> "Consistency";
            case "revision" -> "Revision";
            case "accuracy" -> "Accuracy";
            case "speed" -> "Speed";
            default -> metric;
        };
    }
}
