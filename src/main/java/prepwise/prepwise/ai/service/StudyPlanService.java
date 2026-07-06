package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.StudyPlanRequest;
import prepwise.prepwise.ai.dto.StudyPlanResponse;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.intelligence.UserAnalyticsSnapshot;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.ai.util.CompanyPatternWeights;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

        // Build sorted gap list with coverage data
        List<Map.Entry<String, Double>> gaps = new ArrayList<>();
        Map<String, Double> coverageByTopic = new LinkedHashMap<>();
        for (var e : weights.entrySet()) {
            if ("Others".equals(e.getKey())) continue;
            double coverage = extractor.topicCoveragePercent(snapshot, e.getKey());
            coverageByTopic.put(e.getKey(), coverage);
            double weightedGap = e.getValue() * ((100 - coverage) / 100.0);
            gaps.add(Map.entry(e.getKey(), weightedGap));
        }
        gaps.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> priorityTopics = gaps.stream().map(Map.Entry::getKey).limit(5).toList();

        // Build week plans with daily tasks and expected gains
        List<StudyPlanResponse.WeekPlan> weeks = new ArrayList<>();
        boolean finalWeek = false;
        for (int w = 1; w <= totalWeeks; w++) {
            String focus = priorityTopics.get((w - 1) % priorityTopics.size());
            int weekDays = Math.min(7, Math.max(1, daysRemaining - ((w - 1) * 7)));
            finalWeek = (w == totalWeeks);
            double coverage = coverageByTopic.getOrDefault(focus, 0.0);
            double gap = 100 - coverage;
            double expectedGain = Math.round((gap * weights.getOrDefault(focus, 15) / 2000.0) * 10.0) / 10.0;

            String reason = buildWeekReason(focus, coverage, weights.getOrDefault(focus, 0), finalWeek);
            List<String> dailyTasks = buildDailyTasks(focus, problemsPerDay, finalWeek);

            weeks.add(StudyPlanResponse.WeekPlan.builder()
                    .week(w)
                    .focusTopics(List.of(focus))
                    .problemsPerDay(problemsPerDay)
                    .estimatedHours(request.getHoursPerDay() * weekDays)
                    .focusReason(reason)
                    .expectedReadinessGain(expectedGain)
                    .dailyTasks(dailyTasks)
                    .build());
        }

        // Compact summary for LLM
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("targetCompany", request.getTargetCompany());
        summary.put("daysRemaining", daysRemaining);
        summary.put("totalHours", totalHours);
        summary.put("hoursPerDay", request.getHoursPerDay());
        summary.put("priorityTopics", priorityTopics.stream()
                .map(t -> Map.of("topic", t, "coverage", coverageByTopic.getOrDefault(t, 0.0).intValue(),
                        "companyWeight", weights.getOrDefault(t, 0)))
                .toList());

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

    private String buildWeekReason(String focus, double coverage, int companyWeight, boolean isFinal) {
        if (isFinal) {
            return "Final week: mixed revision and timed practice across all topics.";
        }
        if (coverage < 30) {
            return focus + " coverage is " + (int) coverage + "% vs " + companyWeight
                    + "% company weight — highest-gap topic this week.";
        }
        return focus + " coverage at " + (int) coverage + "% — " + companyWeight
                + "% weight in " + "target company interviews. Closing this gap improves readiness.";
    }

    private List<String> buildDailyTasks(String focus, int problemsPerDay, boolean isFinal) {
        if (isFinal) {
            return List.of(
                    "Timed problem set: 2 mixed-difficulty problems under 45 min",
                    "Revise 2 high-priority problems from History",
                    "Mock interview simulation for 30 min"
            );
        }
        return List.of(
                "Solve " + problemsPerDay + " " + focus + " problem" + (problemsPerDay > 1 ? "s" : "") + " without hints",
                "Review " + focus + " patterns and templates (15 min)",
                "Revise 1 previously solved " + focus + " problem"
        );
    }

    private int calculateProblemsPerDay(String experienceLevel, int hoursPerDay) {
        int baseline = switch (experienceLevel.toUpperCase()) {
            case "BEGINNER" -> 2;
            case "ADVANCED" -> 5;
            default -> 3;
        };
        return Math.max(1, Math.min(8, Math.min(baseline, hoursPerDay * 2)));
    }
}
