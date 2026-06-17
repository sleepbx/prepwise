package prepwise.prepwise.ai.intelligence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import prepwise.prepwise.entity.*;
import prepwise.prepwise.service.MetadataLoader;
import prepwise.prepwise.service.StreakService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static prepwise.prepwise.ai.util.IntelligenceScoring.*;

@Component
@RequiredArgsConstructor
public class AnalyticsFeatureExtractor {

    private static final int COVERAGE_BENCHMARK = 12;
    private static final Map<String, Topic> COMPANY_TOPIC_MAP = Map.ofEntries(
            Map.entry("Graphs", Topic.GRAPH), Map.entry("DP", Topic.DP),
            Map.entry("Trees", Topic.TREE), Map.entry("Arrays", Topic.ARRAY),
            Map.entry("Greedy", Topic.GREEDY), Map.entry("Heap", Topic.HEAP),
            Map.entry("Strings", Topic.STRING), Map.entry("BinarySearch", Topic.BINARY_SEARCH)
    );

    private final StreakService streakService;
    private final MetadataLoader metadataLoader;

    public UserAnalyticsSnapshot extract(User user, List<ProblemAttempt> attempts) {
        Streak streak = streakService.getStreak(user);
        Set<String> uniqueKeys = attempts.stream()
                .map(a -> a.getPlatform().name() + ":" + a.getSlug().toLowerCase())
                .collect(Collectors.toSet());

        long hintCount = attempts.stream().filter(ProblemAttempt::isHintUsed).count();
        double hintRate = attempts.isEmpty() ? 0 : (hintCount * 100.0) / attempts.size();
        double avgTime = attempts.isEmpty() ? 0 :
                attempts.stream().mapToInt(ProblemAttempt::getTimeSpentMinutes).average().orElse(0);

        double revisionRate = computeRevisionRate(attempts);
        double consistency = computeConsistency(attempts);

        Map<Topic, TopicMetrics> topicMetrics = new EnumMap<>(Topic.class);
        for (Topic topic : Topic.values()) {
            if (topic == Topic.UNKNOWN) continue;
            topicMetrics.put(topic, buildTopicMetrics(topic, attempts, consistency));
        }

        return new UserAnalyticsSnapshot(
                attempts.size(), uniqueKeys.size(), round1(hintRate), round1(avgTime),
                round1(revisionRate), round1(consistency),
                streak.getCurrentStreak(), topicMetrics
        );
    }

    public double topicCoveragePercent(UserAnalyticsSnapshot snapshot, String companyArea) {
        Topic topic = COMPANY_TOPIC_MAP.get(companyArea);
        if (topic == null) return snapshot.topicMetrics().values().stream()
                .filter(m -> m.topic() != Topic.UNKNOWN)
                .mapToDouble(TopicMetrics::coverage)
                .average().orElse(0);

        TopicMetrics m = snapshot.topicMetrics().get(topic);
        return m != null ? m.coverage() : 0;
    }

    public double patternCoverage(UserAnalyticsSnapshot snapshot, String patternKey) {
        if (!"TopK".equals(patternKey)) return topicCoveragePercent(snapshot, patternKey);
        // Top K measured via pattern - approximate from TOP_K pattern solves
        return Math.min(100, snapshot.uniqueProblems() * 2.0); // simplified
    }

    public int blind75Solved(User user, List<ProblemAttempt> attempts) {
        Set<String> solved = attempts.stream()
                .map(a -> a.getPlatform().name() + ":" + a.getSlug().toLowerCase())
                .collect(Collectors.toSet());
        var blind = metadataLoader.getSheetMap().get("Blind 75");
        if (blind == null) return 0;
        return (int) blind.stream()
                .filter(e -> solved.contains(e.platform().name() + ":" + e.slug().toLowerCase()))
                .count();
    }

    private TopicMetrics buildTopicMetrics(Topic topic, List<ProblemAttempt> attempts, double globalConsistency) {
        List<ProblemAttempt> filtered = attempts.stream()
                .filter(a -> a.getTopic() == topic).toList();

        int solved = (int) filtered.stream()
                .map(a -> a.getPlatform().name() + ":" + a.getSlug())
                .distinct().count();

        double coverage = Math.min(100, (solved * 100.0) / COVERAGE_BENCHMARK);
        long hints = filtered.stream().filter(ProblemAttempt::isHintUsed).count();
        double hintRate = filtered.isEmpty() ? 0 : (hints * 100.0) / filtered.size();
        double accuracy = 100 - hintRate;
        double avgTime = filtered.isEmpty() ? 0 :
                filtered.stream().mapToInt(ProblemAttempt::getTimeSpentMinutes).average().orElse(0);
        double revision = filtered.isEmpty() ? 0 : computeRevisionRate(filtered);
        double consistency = filtered.isEmpty() ? globalConsistency * 0.5 : computeConsistency(filtered);

        double lowCoverage = 100 - coverage;
        double highSolveTime = Math.min(100, avgTime * 1.2);
        double poorRevision = 100 - revision;
        double inconsistency = 100 - consistency;

        double wIndex = weaknessIndex(lowCoverage, highSolveTime, hintRate, poorRevision, inconsistency);

        return new TopicMetrics(topic, formatTopic(topic), solved, round1(coverage), round1(accuracy),
                round1(avgTime), round1(hintRate), round1(revision), round1(consistency), round1(wIndex));
    }

    private double computeRevisionRate(List<ProblemAttempt> attempts) {
        Map<String, Long> counts = attempts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getPlatform().name() + ":" + a.getSlug().toLowerCase(),
                        Collectors.counting()));
        if (counts.isEmpty()) return 0;
        long revised = counts.values().stream().filter(c -> c > 1).count();
        return (revised * 100.0) / counts.size();
    }

    private double computeConsistency(List<ProblemAttempt> attempts) {
        LocalDate start = LocalDate.now().minusDays(29);
        Set<LocalDate> activeDays = attempts.stream()
                .map(a -> a.getSolvedAt().toLocalDate())
                .filter(d -> !d.isBefore(start))
                .collect(Collectors.toSet());
        return (activeDays.size() * 100.0) / 30.0;
    }

    public String formatTopic(Topic topic) {
        return Arrays.stream(topic.name().split("_"))
                .map(s -> s.charAt(0) + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public record ProblemRevisionStats(
            Long problemId, String title, String slug, Platform platform,
            Difficulty difficulty, int timeSpentMinutes, boolean hintUsed,
            long daysSinceSolved, int revisionCount, double priorityScore, String priorityLevel
    ) {}

    public List<ProblemRevisionStats> extractRevisionStats(List<ProblemAttempt> attempts) {
        Map<String, List<ProblemAttempt>> grouped = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getPlatform().name() + ":" + a.getSlug().toLowerCase()));

        LocalDate today = LocalDate.now();
        List<ProblemRevisionStats> stats = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            List<ProblemAttempt> group = entry.getValue().stream()
                    .sorted(Comparator.comparing(ProblemAttempt::getSolvedAt).reversed())
                    .toList();
            ProblemAttempt latest = group.get(0);
            long daysSince = ChronoUnit.DAYS.between(latest.getSolvedAt().toLocalDate(), today);
            int revisions = group.size() - 1;

            double timeDiff = switch (latest.getDifficulty()) {
                case EASY -> Math.min(100, latest.getTimeSpentMinutes() * 2.0);
                case MEDIUM -> Math.min(100, latest.getTimeSpentMinutes() * 1.2);
                case HARD -> Math.min(100, latest.getTimeSpentMinutes());
                default -> 50;
            };
            double hintDep = latest.isHintUsed() ? 85 : 20;
            double revDeficit = revisions == 0 ? 90 : Math.max(10, 80 - revisions * 25);
            double recency = Math.min(100, daysSince * 8.0);

            double priority = revisionPriority(timeDiff, hintDep, revDeficit, recency);

            stats.add(new ProblemRevisionStats(
                    latest.getId(), latest.getTitle(), latest.getSlug(), latest.getPlatform(),
                    latest.getDifficulty(), latest.getTimeSpentMinutes(), latest.isHintUsed(),
                    daysSince, revisions, round1(priority), revisionLevel(priority)
            ));
        }
        stats.sort(Comparator.comparingDouble(ProblemRevisionStats::priorityScore).reversed());
        return stats;
    }
}
