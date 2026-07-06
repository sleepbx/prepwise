package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DailyCoachResponse {
    private String headline;
    private List<String> tasks;
    private String estimatedImpact;
    private String message;

    // Enriched fields
    private String yesterdaySummary;    // e.g., "3 problems solved, 2 hints used"
    private String highestROITask;      // single highest-impact action today
    private String revisionReminder;    // e.g., "Two Merge Intervals due for review"
    private int currentStreak;
    private String warning;             // e.g., "Streak at risk — no solve logged today"
    private String measurableGoal;      // e.g., "Solve 2 Graph problems without hints"
}
