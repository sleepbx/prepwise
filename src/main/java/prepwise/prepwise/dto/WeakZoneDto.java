package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prepwise.prepwise.entity.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeakZoneDto {
    private Pattern pattern;
    private String label;
    private String reason;
}
