package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RevisionPriorityDto {
    private Long problemId;
    private String title;
    private String slug;
    private String priority;
    private double priorityScore;
    private String reason;

    // Enriched fields
    private String urgencyCategory;  // IMMEDIATE | THIS_WEEK | LATER
    private String difficulty;       // EASY | MEDIUM | HARD
    private long daysSinceSolved;
    private int revisionCount;
    private String confidence;       // HIGH | MEDIUM | LOW (how confident the user was)
}
