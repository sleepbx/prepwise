package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDto {
    private String key;
    private String name;
    private String description;
    private boolean unlocked;
    private int progress;
    private int target;
}
