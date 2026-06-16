package prepwise.prepwise.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import prepwise.prepwise.entity.*;

@Data
@AllArgsConstructor
public class ProblemMetadata {
    private String title;
    private Difficulty difficulty;
    private Topic topic;
    private Pattern pattern;
}
