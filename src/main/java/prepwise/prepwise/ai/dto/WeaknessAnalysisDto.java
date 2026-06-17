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
}
