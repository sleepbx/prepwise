package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prepwise.prepwise.entity.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemAttemptResponse {

    private Long id;
    private String problemUrl;
    private Platform platform;
    private String slug;
    private String title;
    private Difficulty difficulty;
    private Topic topic;
    private Pattern pattern;
    private int timeSpentMinutes;
    private boolean hintUsed;
    private LocalDateTime solvedAt;
}
