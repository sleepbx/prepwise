package prepwise.prepwise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prepwise.prepwise.dto.ReviewQueueDto;
import prepwise.prepwise.entity.*;
import prepwise.prepwise.repository.ProblemAttemptRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PracticeServiceReviewQueueTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ProblemService problemService = mock(ProblemService.class);
    private final PracticeService practiceService = new PracticeService(
            currentUserService, problemService, mock(MetadataLoader.class), mock(ProblemAttemptRepository.class));
    private final User user = new User();
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    private ProblemAttempt solvedDaysAgo(String title, int days, boolean hintUsed) {
        return ProblemAttempt.builder()
                .id(nextId++)
                .user(user)
                .title(title)
                .slug(title.toLowerCase())
                .problemUrl("https://leetcode.com/problems/" + title.toLowerCase() + "/")
                .platform(Platform.values()[0])
                .difficulty(Difficulty.MEDIUM)
                .topic(Topic.values()[0])
                .pattern(Pattern.values()[0])
                .timeSpentMinutes(20)
                .hintUsed(hintUsed)
                .solvedAt(LocalDateTime.now().minusDays(days))
                .build();
    }

    private ReviewQueueDto item(List<ReviewQueueDto> queue, String title) {
        return queue.stream().filter(i -> i.getProblem().equals(title)).findFirst().orElse(null);
    }

    @Test
    void problemSolvedTodayIsNotDueYet() {
        when(problemService.getUserAttempts(user)).thenReturn(List.of(solvedDaysAgo("Today", 0, false)));

        assertThat(practiceService.getReviewQueue()).isEmpty();
    }

    @Test
    void dueDateFollowsOneSevenFourteenThirtyDayIntervals() {
        when(problemService.getUserAttempts(user)).thenReturn(List.of(
                solvedDaysAgo("OneDay", 1, false),
                solvedDaysAgo("EightDays", 8, false),
                solvedDaysAgo("FifteenDays", 15, false),
                solvedDaysAgo("FortyDays", 40, false)));

        List<ReviewQueueDto> queue = practiceService.getReviewQueue();
        LocalDate today = LocalDate.now();

        assertThat(item(queue, "OneDay").getReviewDue()).isEqualTo(today.minusDays(1).plusDays(1));
        assertThat(item(queue, "EightDays").getReviewDue()).isEqualTo(today.minusDays(8).plusDays(7));
        assertThat(item(queue, "FifteenDays").getReviewDue()).isEqualTo(today.minusDays(15).plusDays(14));
        assertThat(item(queue, "FortyDays").getReviewDue()).isEqualTo(today.minusDays(40).plusDays(30));
    }

    @Test
    void priorityDependsOnAgeAndHintUse() {
        when(problemService.getUserAttempts(user)).thenReturn(List.of(
                solvedDaysAgo("Fresh", 2, false),
                solvedDaysAgo("FreshWithHint", 2, true),
                solvedDaysAgo("TwoWeeks", 14, false),
                solvedDaysAgo("Month", 30, false)));

        List<ReviewQueueDto> queue = practiceService.getReviewQueue();

        assertThat(item(queue, "Fresh").getPriority()).isEqualTo("LOW");
        assertThat(item(queue, "FreshWithHint").getPriority()).isEqualTo("HIGH");
        assertThat(item(queue, "TwoWeeks").getPriority()).isEqualTo("MEDIUM");
        assertThat(item(queue, "Month").getPriority()).isEqualTo("HIGH");
    }

    @Test
    void lastReviewDateResetsTheSchedule() {
        ProblemAttempt reviewed = solvedDaysAgo("Reviewed", 40, false);
        reviewed.setLastReviewedAt(LocalDateTime.now().minusDays(2));
        when(problemService.getUserAttempts(user)).thenReturn(List.of(reviewed));

        ReviewQueueDto dto = practiceService.getReviewQueue().get(0);

        assertThat(dto.getReviewDue()).isEqualTo(LocalDate.now().minusDays(2).plusDays(1));
        assertThat(dto.getPriority()).isEqualTo("LOW");
    }

    @Test
    void queueIsSortedByDueDateAndCappedAtEight() {
        List<ProblemAttempt> attempts = new ArrayList<>();
        for (int days = 1; days <= 12; days++) {
            attempts.add(solvedDaysAgo("P" + days, days, false));
        }
        when(problemService.getUserAttempts(user)).thenReturn(attempts);

        List<ReviewQueueDto> queue = practiceService.getReviewQueue();

        assertThat(queue).hasSize(8);
        assertThat(queue).extracting(ReviewQueueDto::getReviewDue).isSorted();
    }
}
