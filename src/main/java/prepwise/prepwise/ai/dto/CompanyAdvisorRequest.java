package prepwise.prepwise.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyAdvisorRequest {
    @NotBlank
    private String company;
}
