package prepwise.prepwise.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudyPlanResponse {
    private int daysRemaining;
    private int totalHours;
    private int totalWeeks;
    private String targetCompany;
    private List<String> priorityTopics;
    private List<WeekPlan> weeks;
    private String explanation;

    @Data
    @Builder
    public static class WeekPlan {
        private int week;
        private List<String> focusTopics;
        private int problemsPerDay;
        private int estimatedHours;
    }
}
