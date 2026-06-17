package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.WeaknessAnalysisDto;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.intelligence.TopicMetrics;
import prepwise.prepwise.ai.intelligence.UserAnalyticsSnapshot;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.ai.util.IntelligenceScoring;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.Topic;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WeaknessAnalysisService {

    private final ProblemService problemService;
    private final CurrentUserService currentUserService;
    private final AnalyticsFeatureExtractor extractor;
    private final LLMService llmService;

    public List<WeaknessAnalysisDto> analyze() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        UserAnalyticsSnapshot snapshot = extractor.extract(user, attempts);

        List<WeaknessAnalysisDto> results = new ArrayList<>();
        List<String> weakTopics = new ArrayList<>();

        for (TopicMetrics m : snapshot.topicMetrics().values()) {
            if (m.solvedCount() == 0 && m.topic() != Topic.ARRAY) continue;
            String classification = IntelligenceScoring.weaknessClassification(m.weaknessIndex());
            String recommendation = buildRecommendation(m, classification);
            if ("WEAK".equals(classification)) weakTopics.add(m.label());

            results.add(WeaknessAnalysisDto.builder()
                    .topic(m.label())
                    .coverage(m.coverage())
                    .accuracy(m.accuracy())
                    .averageSolveTime(m.averageSolveTime())
                    .hintUsageRate(m.hintUsageRate())
                    .revisionCompletionRate(m.revisionCompletionRate())
                    .consistencyScore(m.consistencyScore())
                    .weaknessIndex(m.weaknessIndex())
                    .classification(classification)
                    .recommendation(recommendation)
                    .build());
        }
        results.sort(Comparator.comparingDouble(WeaknessAnalysisDto::getWeaknessIndex).reversed());

        Map<String, Object> summary = Map.of(
                "weakTopics", weakTopics.stream().limit(5).toList(),
                "topWeakness", results.isEmpty() ? "None" : results.get(0).getTopic(),
                "readinessHint", snapshot.consistencyScore()
        );
        String explanation = llmService.generateWeaknessExplanation(summary);
        if (!results.isEmpty()) {
            results.get(0).setRecommendation(explanation);
        }
        return results;
    }

    private String buildRecommendation(TopicMetrics m, String classification) {
        return switch (classification) {
            case "STRONG" -> "Maintain with 1 revision problem per week.";
            case "MODERATE" -> "Solve 2 fresh mediums in " + m.label() + " without hints.";
            default -> "Priority focus: 3 problems/day in " + m.label() + ", then spaced revision.";
        };
    }
}
