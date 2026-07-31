package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.StudyPlanRequest;
import prepwise.prepwise.ai.dto.StudyPlanResponse;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.intelligence.TopicMetrics;
import prepwise.prepwise.ai.intelligence.UserAnalyticsSnapshot;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.ai.util.CompanyPatternWeights;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.Topic;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final ProblemService problemService;
    private final CurrentUserService currentUserService;
    private final AnalyticsFeatureExtractor extractor;
    private final LLMService llmService;

    public StudyPlanResponse generate(StudyPlanRequest request) {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        UserAnalyticsSnapshot snapshot = extractor.extract(user, attempts);

        int daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), request.getInterviewDate());
        daysRemaining = Math.max(daysRemaining, 7);
        int totalHours = daysRemaining * request.getHoursPerDay();
        int totalWeeks = Math.max(1, (int) Math.ceil(daysRemaining / 7.0));
        int problemsPerDay = calculateProblemsPerDay(request.getExperienceLevel(), request.getHoursPerDay());

        Map<String, Integer> weights = CompanyPatternWeights.forCompany(request.getTargetCompany());
        List<FocusArea> focusAreas = buildFocusAreas(snapshot, weights);
        List<String> priorityTopics = focusAreas.stream().map(FocusArea::topic).limit(5).toList();

        List<StudyPlanResponse.WeekPlan> weeks = new ArrayList<>();
        for (int w = 1; w <= totalWeeks; w++) {
            int weekDays = Math.min(7, Math.max(1, daysRemaining - ((w - 1) * 7)));
            boolean finalWeek = w == totalWeeks;
            String phase = resolvePhase(w, totalWeeks);
            FocusArea primary = selectPrimaryFocus(focusAreas, w, finalWeek);
            FocusArea secondary = selectSecondaryFocus(focusAreas, primary, w, finalWeek);
            List<String> focusTopics = secondary == null
                    ? List.of(primary.topic())
                    : List.of(primary.topic(), secondary.topic());

            weeks.add(StudyPlanResponse.WeekPlan.builder()
                    .week(w)
                    .focusTopics(focusTopics)
                    .problemsPerDay(problemsPerDay)
                    .estimatedHours(request.getHoursPerDay() * weekDays)
                    .focusReason(buildWeekReason(primary, secondary, phase, finalWeek))
                    .expectedReadinessGain(estimateWeeklyGain(primary, secondary, request.getHoursPerDay(), weekDays, finalWeek))
                    .dailyTasks(buildDailyTasks(primary, secondary, phase, problemsPerDay, finalWeek))
                    .build());
        }

        Map<String, Object> summary = buildOrchestrationSummary(
                request, snapshot, focusAreas, weeks, daysRemaining, totalHours);

        return StudyPlanResponse.builder()
                .daysRemaining(daysRemaining)
                .totalHours(totalHours)
                .totalWeeks(totalWeeks)
                .targetCompany(request.getTargetCompany())
                .priorityTopics(priorityTopics)
                .weeks(weeks)
                .explanation(llmService.generateStudyPlanExplanation(summary))
                .build();
    }

    private List<FocusArea> buildFocusAreas(UserAnalyticsSnapshot snapshot, Map<String, Integer> weights) {
        List<FocusArea> areas = new ArrayList<>();
        for (var e : weights.entrySet()) {
            if ("Others".equals(e.getKey())) continue;

            TopicMetrics metrics = resolveMetrics(snapshot, e.getKey());
            double coverage = extractor.topicCoveragePercent(snapshot, e.getKey());
            double accuracy = metrics == null ? 100 - snapshot.overallHintRate() : metrics.accuracy();
            double averageSolveTime = metrics == null ? snapshot.averageSolveTime() : metrics.averageSolveTime();
            double hintUsageRate = metrics == null ? snapshot.overallHintRate() : metrics.hintUsageRate();
            double revisionRate = metrics == null ? snapshot.revisionCompletionRate() : metrics.revisionCompletionRate();
            double consistency = metrics == null ? snapshot.consistencyScore() : metrics.consistencyScore();
            double weaknessIndex = metrics == null
                    ? (100 - coverage + hintUsageRate + Math.max(0, averageSolveTime - 35) + (100 - revisionRate)) / 4.0
                    : metrics.weaknessIndex();

            double coverageGap = 100 - coverage;
            double speedPressure = Math.min(100, Math.max(0, averageSolveTime - 35) * 2);
            double priorityScore = (e.getValue() * 1.8)
                    + (coverageGap * 0.45)
                    + (weaknessIndex * 0.35)
                    + (hintUsageRate * 0.2)
                    + ((100 - revisionRate) * 0.15)
                    + ((100 - consistency) * 0.1)
                    + (speedPressure * 0.1);

            areas.add(new FocusArea(
                    e.getKey(),
                    e.getValue(),
                    round1(coverage),
                    round1(coverageGap),
                    round1(accuracy),
                    round1(averageSolveTime),
                    round1(hintUsageRate),
                    round1(revisionRate),
                    round1(consistency),
                    round1(weaknessIndex),
                    round1(priorityScore)
            ));
        }
        areas.sort(Comparator.comparingDouble(FocusArea::priorityScore).reversed());
        return areas;
    }

    private TopicMetrics resolveMetrics(UserAnalyticsSnapshot snapshot, String companyArea) {
        Topic topic = switch (companyArea) {
            case "Graphs" -> Topic.GRAPH;
            case "DP" -> Topic.DP;
            case "Trees" -> Topic.TREE;
            case "Arrays" -> Topic.ARRAY;
            case "Greedy" -> Topic.GREEDY;
            case "Heap", "TopK" -> Topic.HEAP;
            case "Strings" -> Topic.STRING;
            case "BinarySearch" -> Topic.BINARY_SEARCH;
            default -> Topic.UNKNOWN;
        };
        return topic == Topic.UNKNOWN ? null : snapshot.topicMetrics().get(topic);
    }

    private FocusArea selectPrimaryFocus(List<FocusArea> focusAreas, int week, boolean finalWeek) {
        if (finalWeek && focusAreas.size() > 1) {
            return focusAreas.get(0);
        }
        return focusAreas.get((week - 1) % focusAreas.size());
    }

    private FocusArea selectSecondaryFocus(List<FocusArea> focusAreas, FocusArea primary, int week, boolean finalWeek) {
        if (focusAreas.size() < 2) return null;
        if (finalWeek) return focusAreas.get(1);

        return focusAreas.stream()
                .filter(area -> !area.topic().equals(primary.topic()))
                .skip(Math.max(0, (week - 1) % Math.max(1, focusAreas.size() - 1)))
                .findFirst()
                .orElseGet(() -> focusAreas.stream()
                        .filter(area -> !area.topic().equals(primary.topic()))
                        .findFirst()
                        .orElse(null));
    }

    private String resolvePhase(int week, int totalWeeks) {
        double progress = week / (double) totalWeeks;
        if (progress <= 0.4) return "diagnose-and-build";
        if (progress <= 0.75) return "depth-and-speed";
        return "simulation-and-revision";
    }

    private double estimateBaselineReadiness(UserAnalyticsSnapshot snapshot) {
        double coverage = Math.min(100, snapshot.uniqueProblems() * 1.4);
        double accuracy = 100 - snapshot.overallHintRate();
        double revision = snapshot.revisionCompletionRate();
        double consistency = snapshot.consistencyScore();
        return round1((coverage * 0.35) + (accuracy * 0.25) + (revision * 0.2) + (consistency * 0.2));
    }

    private double estimateWeeklyGain(FocusArea primary, FocusArea secondary, int hoursPerDay, int weekDays, boolean finalWeek) {
        double intensity = Math.min(1.4, Math.max(0.6, (hoursPerDay * weekDays) / 21.0));
        double primaryGain = (primary.coverageGap() * primary.companyWeight()) / 1400.0;
        double secondaryGain = secondary == null ? 0 : (secondary.coverageGap() * secondary.companyWeight()) / 2600.0;
        double simulationBonus = finalWeek ? 0.8 : 0.0;
        return round1(Math.min(5.0, (primaryGain + secondaryGain) * intensity + simulationBonus));
    }

    private String buildWeekReason(FocusArea primary, FocusArea secondary, String phase, boolean isFinal) {
        if (isFinal) {
            String secondaryText = secondary == null ? "mixed weak areas" : secondary.topic();
            return "Final week: simulate interviews while revising " + primary.topic() + " and " + secondaryText
                    + ", the highest remaining weighted risks.";
        }
        if (primary.coverage() < 30) {
            return primary.topic() + " coverage is " + (int) primary.coverage() + "% against "
                    + primary.companyWeight() + "% company weight; " + phase
                    + " puts it first because the weighted gap is still large.";
        }
        String secondaryText = secondary == null ? "revision" : secondary.topic() + " retention";
        return primary.topic() + " is at " + (int) primary.coverage() + "% coverage with "
                + primary.companyWeight() + "% company weight; pair it with " + secondaryText
                + " to improve readiness without overfitting one topic.";
    }

    private List<String> buildDailyTasks(FocusArea primary, FocusArea secondary, String phase, int problemsPerDay, boolean isFinal) {
        if (isFinal) {
            return List.of(
                    "Run one timed mock block: 2 mixed problems in 45 minutes with no hints",
                    "Revise 2 high-priority misses from History and write the reusable pattern",
                    "Do a 30-minute verbal walkthrough for " + primary.topic() + " plus one secondary topic"
            );
        }

        String secondaryTask = secondary == null
                ? "Revise 1 previously solved problem from the same topic"
                : "Revise 1 older " + secondary.topic() + " problem to protect retention";
        String qualityGate = primary.hintUsageRate() > 35
                ? "Stop after each miss and write the first hint you needed before checking the solution"
                : "Track first-pass solution time and keep hint-free accuracy as the quality gate";

        return List.of(
                "Solve " + problemsPerDay + " " + primary.topic() + " problem" + (problemsPerDay > 1 ? "s" : "") + " without hints",
                "Spend 15 minutes on the " + primary.topic() + " template for the " + phase + " phase",
                secondaryTask,
                qualityGate
        );
    }

    private Map<String, Object> buildOrchestrationSummary(
            StudyPlanRequest request,
            UserAnalyticsSnapshot snapshot,
            List<FocusArea> focusAreas,
            List<StudyPlanResponse.WeekPlan> weeks,
            int daysRemaining,
            int totalHours) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("targetCompany", request.getTargetCompany());
        summary.put("daysRemaining", daysRemaining);
        summary.put("totalHours", totalHours);
        summary.put("hoursPerDay", request.getHoursPerDay());
        summary.put("experienceLevel", request.getExperienceLevel());
        summary.put("baselineReadiness", estimateBaselineReadiness(snapshot));
        summary.put("candidateSignals", Map.of(
                "totalAttempts", snapshot.totalAttempts(),
                "uniqueProblems", snapshot.uniqueProblems(),
                "hintRate", snapshot.overallHintRate(),
                "averageSolveTime", snapshot.averageSolveTime(),
                "revisionCompletionRate", snapshot.revisionCompletionRate(),
                "consistencyScore", snapshot.consistencyScore(),
                "currentStreak", snapshot.currentStreak()
        ));
        summary.put("priorityTopics", focusAreas.stream().limit(5)
                .map(FocusArea::toPromptMap)
                .toList());
        summary.put("orchestration", Map.of(
                "strategy", "Front-load highest weighted gaps, add weekly secondary topic for retention, finish with mixed timed mocks.",
                "phasePlan", List.of("diagnose-and-build", "depth-and-speed", "simulation-and-revision"),
                "riskControls", buildRiskControls(snapshot),
                "weeklyPlan", weeks.stream()
                        .map(w -> Map.of(
                                "week", w.getWeek(),
                                "focusTopics", w.getFocusTopics(),
                                "estimatedHours", w.getEstimatedHours(),
                                "expectedReadinessGain", w.getExpectedReadinessGain(),
                                "reason", w.getFocusReason()
                        ))
                        .toList()
        ));
        return summary;
    }

    private List<String> buildRiskControls(UserAnalyticsSnapshot snapshot) {
        List<String> risks = new ArrayList<>();
        if (snapshot.overallHintRate() > 30) {
            risks.add("High hint rate: enforce no-hint first attempts and post-solve mistake logs.");
        }
        if (snapshot.revisionCompletionRate() < 40) {
            risks.add("Low revision rate: reserve one task daily for old problems before new volume.");
        }
        if (snapshot.consistencyScore() < 50) {
            risks.add("Low consistency: keep daily scope small enough to avoid missed days.");
        }
        if (snapshot.averageSolveTime() > 45) {
            risks.add("Slow average solve time: add timed blocks after concept reps.");
        }
        if (risks.isEmpty()) {
            risks.add("Current signals are stable; optimize for mixed mocks and harder variants.");
        }
        return risks;
    }

    private int calculateProblemsPerDay(String experienceLevel, int hoursPerDay) {
        int baseline = switch (experienceLevel.toUpperCase()) {
            case "BEGINNER" -> 2;
            case "ADVANCED" -> 5;
            default -> 3;
        };
        return Math.max(1, Math.min(8, Math.min(baseline, hoursPerDay * 2)));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record FocusArea(
            String topic,
            int companyWeight,
            double coverage,
            double coverageGap,
            double accuracy,
            double averageSolveTime,
            double hintUsageRate,
            double revisionCompletionRate,
            double consistencyScore,
            double weaknessIndex,
            double priorityScore
    ) {
        Map<String, Object> toPromptMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("topic", topic);
            map.put("companyWeight", companyWeight);
            map.put("coverage", coverage);
            map.put("coverageGap", coverageGap);
            map.put("accuracy", accuracy);
            map.put("averageSolveTime", averageSolveTime);
            map.put("hintUsageRate", hintUsageRate);
            map.put("revisionCompletionRate", revisionCompletionRate);
            map.put("consistencyScore", consistencyScore);
            map.put("weaknessIndex", weaknessIndex);
            map.put("priorityScore", priorityScore);
            return map;
        }
    }
}
