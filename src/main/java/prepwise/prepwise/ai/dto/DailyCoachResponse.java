package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DailyCoachResponse {
    private String headline;
    private List<String> tasks;
    private String estimatedImpact;
    private String message;
}
