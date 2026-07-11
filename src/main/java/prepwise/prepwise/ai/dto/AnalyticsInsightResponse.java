package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AnalyticsInsightResponse {
    private Map<String, Object> changes;
    private String insight;
    private String trend;          // IMPROVING | STABLE | DECLINING
    private String biggestWin;     // e.g., "Solved 7 more problems than last week"
    private String biggestRisk;    // e.g., "Active days dropped by 3"
}
