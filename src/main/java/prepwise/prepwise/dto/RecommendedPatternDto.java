package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prepwise.prepwise.entity.Pattern;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedPatternDto {
    private Pattern pattern;
    private String label;
    private int confidenceScore;
    private LocalDate lastPracticed;
    private int problemsSolved;
    private String reason;
}
