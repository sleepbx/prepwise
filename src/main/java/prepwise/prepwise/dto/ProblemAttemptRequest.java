package prepwise.prepwise.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProblemAttemptRequest {

    @NotBlank
    private String problemUrl;

    @Min(1)
    private int timeSpentMinutes;

    private boolean hintUsed;
}
