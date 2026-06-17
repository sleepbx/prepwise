package prepwise.prepwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import prepwise.prepwise.entity.Platform;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQueueDto {
    private Long attemptId;
    private String problem;
    private Platform platform;
    private String url;
    private LocalDate solvedDate;
    private LocalDate reviewDue;
    private String priority;
}
