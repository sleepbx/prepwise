package prepwise.prepwise.ai.util;

import prepwise.prepwise.entity.Topic;

import java.util.Map;

public final class IntelligenceScoring {

    // Interview importance by topic (0-100), used to weight recommendations
    public static final Map<Topic, Integer> TOPIC_INTERVIEW_IMPORTANCE = Map.ofEntries(
            Map.entry(Topic.ARRAY, 95),
            Map.entry(Topic.STRING, 85),
            Map.entry(Topic.DP, 85),
            Map.entry(Topic.TREE, 85),
            Map.entry(Topic.GRAPH, 80),
            Map.entry(Topic.BINARY_SEARCH, 75),
            Map.entry(Topic.HEAP, 72),
            Map.entry(Topic.GREEDY, 70),
            Map.entry(Topic.LINKED_LIST, 70),
            Map.entry(Topic.BACKTRACKING, 65),
            Map.entry(Topic.STACK, 65),
            Map.entry(Topic.QUEUE, 55)
    );

    private IntelligenceScoring() {}

    public static double weaknessIndex(double lowCoverage, double highSolveTime, double hintUsage,
                                       double poorRevision, double inconsistency) {
        return clamp(
                0.30 * lowCoverage +
                0.20 * highSolveTime +
                0.20 * hintUsage +
                0.15 * poorRevision +
                0.15 * inconsistency
        );
    }

    public static String weaknessClassification(double index) {
        if (index <= 30) return "STRONG";
        if (index <= 60) return "MODERATE";
        return "WEAK";
    }

    // Coverage ×35%, Consistency ×25%, Revision ×20%, Accuracy ×15%, Speed ×5%
    public static double readinessScore(double coverage, double consistency, double revision,
                                        double accuracy, double speed) {
        return clamp(
                0.35 * coverage + 0.25 * consistency + 0.20 * revision +
                0.15 * accuracy + 0.05 * speed
        );
    }

    public static String readinessLevel(double score) {
        if (score < 35) return "EARLY_STAGE";
        if (score < 60) return "ON_TRACK";
        if (score < 80) return "INTERVIEW_READY";
        return "STRONG_CANDIDATE";
    }

    public static String hiringRisk(double score) {
        if (score >= 75) return "LOW";
        if (score >= 50) return "MEDIUM";
        return "HIGH";
    }

    public static String expectedOutcome(double score) {
        if (score >= 82) return "Strong Candidate";
        if (score >= 70) return "Onsite Ready";
        if (score >= 55) return "Phone Screen Ready";
        if (score >= 38) return "Online Assessment Ready";
        return "Not Ready";
    }

    public static double revisionPriority(double timeDifficulty, double hintDependence,
                                          double revisionDeficit, double recencyFactor) {
        return clamp(
                0.25 * timeDifficulty + 0.25 * hintDependence +
                0.25 * revisionDeficit + 0.25 * recencyFactor
        );
    }

    public static String revisionLevel(double priority) {
        if (priority < 35) return "LOW";
        if (priority < 65) return "MEDIUM";
        return "HIGH";
    }

    public static String urgencyCategory(double priorityScore, long daysSinceSolved) {
        if (priorityScore >= 70 || daysSinceSolved > 21) return "IMMEDIATE";
        if (priorityScore >= 40 || daysSinceSolved > 10) return "THIS_WEEK";
        return "LATER";
    }

    public static int topicImportance(Topic topic) {
        return TOPIC_INTERVIEW_IMPORTANCE.getOrDefault(topic, 60);
    }

    public static String confidenceLevel(double coverage, double accuracy) {
        if (coverage >= 70 && accuracy >= 80) return "HIGH";
        if (coverage >= 40 || accuracy >= 60) return "MEDIUM";
        return "LOW";
    }

    public static String estimatedEffort(String classification, String label) {
        return switch (classification) {
            case "STRONG" -> "1 problem/week for maintenance";
            case "MODERATE" -> "2–3 " + label + " problems/week for 2 weeks";
            default -> "3–4 " + label + " problems/day for 1–2 weeks";
        };
    }

    public static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
