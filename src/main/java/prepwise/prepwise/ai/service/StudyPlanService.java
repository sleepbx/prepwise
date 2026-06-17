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

        Map<String, Integer> weights = CompanyPatternWeights.forCompany(request.getTargetCompany());
        List<Map.Entry<String, Double>> gaps = new ArrayList<>();
        for (var e : weights.entrySet()) {
            if ("Others".equals(e.getKey())) continue;
            double coverage = extractor.topicCoveragePercent(snapshot, e.getKey());
            double weightedGap = e.getValue() * ((100 - coverage) / 100.0);
            gaps.add(Map.entry(e.getKey(), weightedGap));
        }
        gaps.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> priorityTopics = gaps.stream().map(Map.Entry::getKey).limit(5).toList();

        List<StudyPlanResponse.WeekPlan> weeks = new ArrayList<>();
        for (int w = 1; w <= totalWeeks; w++) {
            String focus = priorityTopics.get((w - 1) % priorityTopics.size());
            int problemsPerDay = calculateProblemsPerDay(request.getExperienceLevel(), request.getHoursPerDay());
            int weekDays = Math.min(7, Math.max(1, daysRemaining - ((w - 1) * 7)));
            weeks.add(StudyPlanResponse.WeekPlan.builder()
                    .week(w)
                    .focusTopics(List.of(focus))
                    .problemsPerDay(problemsPerDay)
                    .estimatedHours(request.getHoursPerDay() * weekDays)
                    .build());
        }

        Map<String, Object> summary = Map.of(
                "targetCompany", request.getTargetCompany(),
                "daysRemaining", daysRemaining,
                "totalHours", totalHours,
                "hoursPerDay", request.getHoursPerDay(),
                "priorityTopics", priorityTopics,
                "weeks", weeks.stream().map(w -> Map.of(
                        "week", w.getWeek(),
                        "focusTopics", w.getFocusTopics(),
                        "problemsPerDay", w.getProblemsPerDay()
                )).toList()
        );

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

    private int calculateProblemsPerDay(String experienceLevel, int hoursPerDay) {
        int baseline = experienceLevel.equalsIgnoreCase("BEGINNER") ? 2 :
                experienceLevel.equalsIgnoreCase("ADVANCED") ? 5 : 4;
        return Math.max(1, Math.min(8, Math.min(baseline, hoursPerDay * 2)));
    }
}
