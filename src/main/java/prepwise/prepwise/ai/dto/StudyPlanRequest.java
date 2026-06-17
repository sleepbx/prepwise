package prepwise.prepwise.ai.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudyPlanRequest {
    @NotBlank
    private String targetCompany;

    @NotNull
    private LocalDate interviewDate;

    @Min(1) @Max(12)
    private int hoursPerDay = 3;

    @NotBlank
    private String experienceLevel = "INTERMEDIATE";
}
