package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImprovementDto {
    private int problemsSolvedDiff;
    private double avgSolveTimeDiff;
    private double hintUsageDiff;
    private int last7Problems;
    private int prev7Problems;
    private double last7AvgTime;
    private double prev7AvgTime;
    private double last7HintPercent;
    private double prev7HintPercent;
}
