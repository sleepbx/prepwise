package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReadinessResponse {
    private double score;
    private String level;
    private Map<String, Double> breakdown;
    private List<String> keyStrengths;
    private List<String> improvementAreas;
    private String explanation;
}
