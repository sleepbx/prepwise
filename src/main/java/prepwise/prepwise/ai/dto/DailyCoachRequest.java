package prepwise.prepwise.ai.dto;

import lombok.Data;

@Data
public class DailyCoachRequest {
    private String company = "Google";
    private String prompt = "What should I improve today and why?";
}
