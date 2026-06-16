package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prepwise.prepwise.entity.Pattern;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private int currentStreak;
    private int sprintProgress;
    private int totalSolved;
    private int problemsSolved;
    private int timeInvestedMinutes;
    private double averageSolveTime;
    private double hintUsagePercent;
    private List<SheetProgressDto> sheetProgress;
    private List<PatternCountDto> topPatterns;
    private List<ProblemAttemptResponse> recentAttempts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SheetProgressDto {
        private String name;
        private int solved;
        private int total;
        private double percentage;
        private int patternsCovered;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternCountDto {
        private Pattern pattern;
        private String label;
        private int count;
    }
}
