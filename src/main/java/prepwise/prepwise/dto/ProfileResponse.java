package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String initials;
    private int currentStreak;
    private int longestStreak;
    private int totalSolved;
    private int totalTimeMinutes;
    private String favoritePlatform;
    private String strongestPattern;
    private String weakestPattern;
    private List<HeatmapDayDto> heatmap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapDayDto {
        private String date;
        private int problemsSolved;
        private int minutesInvested;
        private int level;
    }
}
