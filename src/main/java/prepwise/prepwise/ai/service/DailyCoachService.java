package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.DailyCoachRequest;
import prepwise.prepwise.ai.dto.DailyCoachResponse;
import prepwise.prepwise.ai.dto.ReadinessResponse;
import prepwise.prepwise.ai.dto.RevisionPriorityDto;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.intelligence.TopicMetrics;
import prepwise.prepwise.ai.intelligence.UserAnalyticsSnapshot;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.ai.util.CompanyPatternWeights;
import prepwise.prepwise.ai.util.IntelligenceScoring;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

        TopicMetrics weakestTopic = snapshot.topicMetrics().values().stream()
                .filter(m -> m.solvedCount() > 0)
                .max(Comparator.comparingDouble(TopicMetrics::weaknessIndex))
                .orElse(null);
        String weakLabel = weakestTopic != null ? weakestTopic.label() : "Arrays";
        int importance = weakestTopic != null ? IntelligenceScoring.topicImportance(weakestTopic.topic()) : 80;

        List<RevisionPriorityDto> revisions = revisionPriorityService.getPriorities().stream().limit(3).toList();
        List<String> tasks = buildTasks(weakLabel, revisions, company);

        String yesterdaySummary = buildYesterdaySummary(attempts);
        String highestROI = "Solve 2 " + weakLabel + " mediums without hints (" + importance + "% interview importance)";
        String revisionReminder = revisions.isEmpty() ? null
                : revisions.get(0).getTitle() + " is due for revision (" + revisions.get(0).getDaysSinceSolved() + " days ago)";
        String warning = buildWarning(snapshot, attempts);
        String measurableGoal = "Solve 2 " + weakLabel + " problems without hints by end of day";
        String headline = "Focus on " + weakLabel + " today — " + importance + "% interview importance for " + company + ".";
        String impact = estimateImpact(readiness.getScore(), revisions.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userPrompt", userPrompt);
        summary.put("company", company);
        summary.put("companyWeights", CompanyPatternWeights.forCompany(company));
        summary.put("currentStreak", snapshot.currentStreak());
        summary.put("readinessScore", readiness.getScore());
        summary.put("weakTopic", weakLabel);
        summary.put("topicImportance", importance);
        summary.put("revisionsDue", revisions.size());
        summary.put("hiringRisk", readiness.getHiringRisk());

        return DailyCoachResponse.builder()
                .headline(headline)
                .tasks(tasks)
                .estimatedImpact(impact)
                .message(llmService.generateDailyCoachMessage(summary))
                .yesterdaySummary(yesterdaySummary)
                .highestROITask(highestROI)
                .revisionReminder(revisionReminder)
                .currentStreak(snapshot.currentStreak())
                .warning(warning)
                .measurableGoal(measurableGoal)
                .build();
    }

    private List<String> buildTasks(String weakLabel, List<RevisionPriorityDto> revisions, String company) {
        List<String> tasks = new ArrayList<>();
        if (!revisions.isEmpty()) {
            tasks.add("Revise: " + revisions.get(0).getTitle() + " (due " + revisions.get(0).getDaysSinceSolved() + " days ago)");
        } else {
            tasks.add("Pick 2 high-priority problems from your History to revise");
        }
        tasks.add("Solve 2 fresh " + weakLabel + " problems without hints");
        tasks.add("Log at least 1 attempt to protect your streak");
        return tasks;
    }

    private String buildYesterdaySummary(List<ProblemAttempt> attempts) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<ProblemAttempt> yesterdayAttempts = attempts.stream()
                .filter(a -> a.getSolvedAt().toLocalDate().equals(yesterday))
                .toList();
        if (yesterdayAttempts.isEmpty()) return "No problems logged yesterday.";
        long hints = yesterdayAttempts.stream().filter(ProblemAttempt::isHintUsed).count();
        return yesterdayAttempts.size() + " problem" + (yesterdayAttempts.size() == 1 ? "" : "s") + " solved"
                + (hints > 0 ? ", " + hints + " hint" + (hints == 1 ? "" : "s") + " used" : ", no hints used") + ".";
    }

    private String buildWarning(UserAnalyticsSnapshot snapshot, List<ProblemAttempt> attempts) {
        boolean solvedToday = attempts.stream()
                .anyMatch(a -> a.getSolvedAt().toLocalDate().equals(LocalDate.now()));
        if (!solvedToday && snapshot.currentStreak() > 0) {
            return "Streak at risk — no solve logged today yet.";
        }
        if (snapshot.revisionCompletionRate() < 20) {
            return "Revision rate is " + (int) snapshot.revisionCompletionRate() + "% — problems forgotten quickly without review.";
        }
        return null;
    }

    private String normalizeCompany(String company) {
        if (company == null || company.isBlank()) return "Google";
        String t = company.trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase();
    }

    private String estimateImpact(double readinessScore, int revisionCount) {
        if (readinessScore < 40) return "+4–5 readiness points";
        if (readinessScore < 60) return "+2–3 readiness points";
        if (revisionCount > 0) return "+1–2 readiness points";
        return "+0.5–1 readiness point";
    }
}
