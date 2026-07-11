package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeaknessAnalysisDto {
    private String topic;
    private double coverage;
    private double accuracy;
    private double averageSolveTime;
    private double hintUsageRate;
    private double revisionCompletionRate;
    private double consistencyScore;
    private double weaknessIndex;
    private String classification;
    private String recommendation;

    // Enriched analytics fields
    private int interviewImportance;   // 0–100, how often this topic appears in FAANG interviews
    private String confidence;          // HIGH | MEDIUM | LOW, based on coverage + accuracy
    private String estimatedEffort;     // e.g., "2–3 problems/week for 2 weeks"
    private String readinessImpact;     // e.g., "+3.5 points if addressed"
    private String recentTrend;         // IMPROVING | STABLE | DECLINING
}
