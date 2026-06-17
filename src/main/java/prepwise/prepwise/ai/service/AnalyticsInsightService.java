package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.AnalyticsInsightResponse;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.dto.ImprovementDto;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.service.AnalyticsService;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsInsightService {

    private final AnalyticsService analyticsService;
    private final ProblemService problemService;
    private final CurrentUserService currentUserService;
    private final LLMService llmService;

    public AnalyticsInsightResponse generateInsight() {
        ImprovementDto imp = analyticsService.getImprovement();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(currentUserService.getCurrentUser());
        int last7ActiveDays = activeDays(attempts, LocalDate.now().minusDays(6), LocalDate.now());
        int prev7ActiveDays = activeDays(attempts, LocalDate.now().minusDays(13), LocalDate.now().minusDays(7));

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("problemsSolvedDiff", imp.getProblemsSolvedDiff());
        changes.put("last7Problems", imp.getLast7Problems());
        changes.put("prev7Problems", imp.getPrev7Problems());
        changes.put("avgSolveTimeDiff", imp.getAvgSolveTimeDiff());
        changes.put("hintUsageDiff", imp.getHintUsageDiff());
        changes.put("activeDaysDiff", last7ActiveDays - prev7ActiveDays);
        changes.put("last7ActiveDays", last7ActiveDays);
        changes.put("prev7ActiveDays", prev7ActiveDays);

        Map<String, Object> summary = Map.of(
                "problemsSolvedDiff", imp.getProblemsSolvedDiff(),
                "avgSolveTimeDiff", imp.getAvgSolveTimeDiff(),
                "hintUsageDiff", imp.getHintUsageDiff(),
                "last7Problems", imp.getLast7Problems(),
                "prev7Problems", imp.getPrev7Problems(),
                "activeDaysDiff", last7ActiveDays - prev7ActiveDays
        );

        return AnalyticsInsightResponse.builder()
                .changes(changes)
                .insight(llmService.generateAnalyticsInsight(summary))
                .build();
    }

    private int activeDays(List<ProblemAttempt> attempts, LocalDate start, LocalDate end) {
        Set<LocalDate> days = attempts.stream()
                .map(a -> a.getSolvedAt().toLocalDate())
                .filter(d -> !d.isBefore(start) && !d.isAfter(end))
                .collect(Collectors.toSet());
        return days.size();
    }
}
