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
}
