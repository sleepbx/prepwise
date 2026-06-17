package prepwise.prepwise.ai.util;

public final class IntelligenceScoring {

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

    public static double readinessScore(double coverage, double consistency, double revision,
                                        double accuracy, double speed) {
        return clamp(
                0.35 * coverage + 0.25 * consistency + 0.15 * revision +
                0.15 * accuracy + 0.10 * speed
        );
    }

    public static String readinessLevel(double score) {
        if (score < 40) return "EARLY_STAGE";
        if (score < 70) return "ON_TRACK";
        if (score < 85) return "INTERVIEW_READY";
        return "STRONG_CANDIDATE";
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

    public static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
