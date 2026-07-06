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

        int solvedDiff = imp.getProblemsSolvedDiff();
        int activeDiff = last7ActiveDays - prev7ActiveDays;
        String trend = deriveTrend(solvedDiff, activeDiff);
        String biggestWin = deriveBiggestWin(imp, last7ActiveDays, prev7ActiveDays);
        String biggestRisk = deriveBiggestRisk(imp, last7ActiveDays, prev7ActiveDays);

        return AnalyticsInsightResponse.builder()
                .changes(changes)
                .insight(llmService.generateAnalyticsInsight(summary))
                .trend(trend)
                .biggestWin(biggestWin)
                .biggestRisk(biggestRisk)
                .build();
    }

    private int activeDays(List<ProblemAttempt> attempts, LocalDate start, LocalDate end) {
        Set<LocalDate> days = attempts.stream()
                .map(a -> a.getSolvedAt().toLocalDate())
                .filter(d -> !d.isBefore(start) && !d.isAfter(end))
                .collect(Collectors.toSet());
        return days.size();
    }

    private String deriveTrend(int solvedDiff, int activeDiff) {
        if (solvedDiff > 2 || activeDiff > 1) return "IMPROVING";
        if (solvedDiff < -2 || activeDiff < -2) return "DECLINING";
        return "STABLE";
    }

    private String deriveBiggestWin(ImprovementDto imp, int last7Active, int prev7Active) {
        if (imp.getProblemsSolvedDiff() > 3)
            return "Solved " + imp.getProblemsSolvedDiff() + " more problems than last week";
        if (last7Active > prev7Active)
            return "Active " + (last7Active - prev7Active) + " more day(s) than last week";
        if (imp.getAvgSolveTimeDiff() < -5)
            return "Avg solve time improved by " + Math.abs((int) imp.getAvgSolveTimeDiff()) + " minutes";
        return "Maintained preparation consistency";
    }

    private String deriveBiggestRisk(ImprovementDto imp, int last7Active, int prev7Active) {
        if (imp.getProblemsSolvedDiff() < -3)
            return "Problem volume dropped by " + Math.abs(imp.getProblemsSolvedDiff()) + " vs. last week";
        if (last7Active < prev7Active - 2)
            return "Active days dropped by " + (prev7Active - last7Active) + " — consistency at risk";
        if (imp.getHintUsageDiff() > 15)
            return "Hint usage increased by " + (int) imp.getHintUsageDiff() + "% — accuracy may be declining";
        return null;
    }
}
