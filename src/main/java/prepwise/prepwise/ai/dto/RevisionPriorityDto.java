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
}
