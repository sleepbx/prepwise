package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prepwise.prepwise.entity.Difficulty;
import prepwise.prepwise.entity.Pattern;
import prepwise.prepwise.entity.Platform;
import prepwise.prepwise.entity.Topic;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProblemDto {
    private Long attemptId;
    private String title;
    private Platform platform;
    private String slug;
    private String url;
    private Difficulty difficulty;
    private Topic topic;
    private Pattern pattern;
    private String sheet;
    private int estimatedSolveTimeMinutes;
    private String reason;
}
