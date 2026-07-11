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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
            int importance = IntelligenceScoring.topicImportance(m.topic());
            String confidence = IntelligenceScoring.confidenceLevel(m.coverage(), m.accuracy());
            String effort = IntelligenceScoring.estimatedEffort(classification, m.label());
            double impactPoints = IntelligenceScoring.round1(m.weaknessIndex() * importance / 2500.0 * 10);
            String readinessImpact = "+" + impactPoints + " points if addressed";
            String trend = computeTrend(m.topic(), attempts);

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
                    .recommendation(buildRecommendation(m, classification))
                    .interviewImportance(importance)
                    .confidence(confidence)
                    .estimatedEffort(effort)
                    .readinessImpact(readinessImpact)
                    .recentTrend(trend)
                    .build());
        }
        results.sort(Comparator.comparingDouble(WeaknessAnalysisDto::getWeaknessIndex).reversed());

        if (!results.isEmpty()) {
            WeaknessAnalysisDto top = results.get(0);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("topWeakness", top.getTopic());
            summary.put("coverage", top.getCoverage());
            summary.put("accuracy", top.getAccuracy());
            summary.put("avgSolveTime", top.getAverageSolveTime());
            summary.put("hintRate", top.getHintUsageRate());
            summary.put("interviewImportance", top.getInterviewImportance());
            summary.put("weaknessIndex", top.getWeaknessIndex());
            summary.put("weakTopics", weakTopics.stream().limit(5).toList());
            top.setRecommendation(llmService.generateWeaknessExplanation(summary));
        }
        return results;
    }

    private String buildRecommendation(TopicMetrics m, String classification) {
        return switch (classification) {
            case "STRONG" -> m.label() + " coverage at " + (int) m.coverage() + "%. Maintain with 1 revision problem per week.";
            case "MODERATE" -> {
                String timeNote = m.averageSolveTime() > 35
                        ? " Avg solve time " + (int) m.averageSolveTime() + "m — focus on pattern recognition first."
                        : "";
                yield m.label() + " coverage at " + (int) m.coverage() + "%, accuracy " + (int) m.accuracy()
                      + "%. Solve 2–3 mediums without hints this week." + timeNote;
            }
            default -> {
                String timeNote = m.averageSolveTime() > 40
                        ? " Avg " + (int) m.averageSolveTime() + "m per problem indicates concept gaps, not practice volume — study patterns before grinding."
                        : " Solve 3 problems/day with no hints, then revise within 48 hours.";
                yield m.label() + " coverage is " + (int) m.coverage()
                      + "% — this is your highest readiness drag." + timeNote;
            }
        };
    }

    private String computeTrend(Topic topic, List<ProblemAttempt> attempts) {
        LocalDate cutoff = LocalDate.now().minusDays(14);
        LocalDate prevCutoff = LocalDate.now().minusDays(28);
        long recent = attempts.stream()
                .filter(a -> a.getTopic() == topic)
                .filter(a -> !a.getSolvedAt().toLocalDate().isBefore(cutoff))
                .count();
        long previous = attempts.stream()
                .filter(a -> a.getTopic() == topic)
                .filter(a -> a.getSolvedAt().toLocalDate().isBefore(cutoff)
                        && !a.getSolvedAt().toLocalDate().isBefore(prevCutoff))
                .count();
        if (recent > previous + 1) return "IMPROVING";
        if (recent < previous - 1) return "DECLINING";
        return "STABLE";
    }
}
