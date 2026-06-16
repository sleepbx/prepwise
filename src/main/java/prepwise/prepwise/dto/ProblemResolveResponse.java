package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prepwise.prepwise.entity.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResolveResponse {
    private String problemUrl;
    private Platform platform;
    private String slug;
    private String title;
    private Difficulty difficulty;
    private Topic topic;
    private Pattern pattern;
    private boolean found;
    private String message;
}
