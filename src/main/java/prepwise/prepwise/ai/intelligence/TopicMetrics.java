package prepwise.prepwise.ai.intelligence;

import prepwise.prepwise.entity.Topic;

public record TopicMetrics(
        Topic topic,
        String label,
        int solvedCount,
        double coverage,
        double accuracy,
        double averageSolveTime,
        double hintUsageRate,
        double revisionCompletionRate,
        double consistencyScore,
        double weaknessIndex
) {}
