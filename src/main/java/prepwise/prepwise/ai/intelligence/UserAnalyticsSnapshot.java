package prepwise.prepwise.ai.intelligence;

import prepwise.prepwise.entity.Topic;

import java.util.Map;

public record UserAnalyticsSnapshot(
        int totalAttempts,
        int uniqueProblems,
        double overallHintRate,
        double averageSolveTime,
        double revisionCompletionRate,
        double consistencyScore,
        int currentStreak,
        Map<Topic, TopicMetrics> topicMetrics
) {}
