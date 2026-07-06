package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CompanyAdvisorResponse {
    private String company;
    private List<String> focusAreas;
    private Map<String, String> weeklyAllocation;
    private String explanation;

    // Enriched fields
    private Map<String, TopicGap> topicComparison;
    private List<String> topicsAlreadySufficient;
    private List<String> topicsOverPracticed;
    private List<HighROIItem> highROIItems;

    @Data
    @Builder
    public static class TopicGap {
        private int expected;       // company's expected % for this topic
        private int current;        // user's current coverage %
        private int gap;            // expected - current (clamped to >= 0)
    }

    @Data
    @Builder
    public static class HighROIItem {
        private String topic;
        private String expectedGain;  // e.g., "+4–6 readiness points"
        private String effortLevel;   // LOW | MEDIUM | HIGH
    }
}
