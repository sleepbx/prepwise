package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AnalyticsInsightResponse {
    private Map<String, Object> changes;
    private String insight;
}
