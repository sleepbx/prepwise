package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.RevisionPriorityDto;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevisionPriorityService {

    private final ProblemService problemService;
    private final CurrentUserService currentUserService;
    private final AnalyticsFeatureExtractor extractor;

    public List<RevisionPriorityDto> getPriorities() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);

        return extractor.extractRevisionStats(attempts).stream()
                .limit(15)
                .map(s -> RevisionPriorityDto.builder()
                        .problemId(s.problemId())
                        .title(s.title())
                        .slug(s.slug())
                        .priority(s.priorityLevel())
                        .priorityScore(s.priorityScore())
                        .reason(buildReason(s))
                        .build())
                .toList();
    }

    private String buildReason(AnalyticsFeatureExtractor.ProblemRevisionStats s) {
        if (s.revisionCount() == 0 && s.daysSinceSolved() > 14) {
            return "Never revised after " + s.daysSinceSolved() + " days.";
        }
        if (s.hintUsed() && s.timeSpentMinutes() > 40) {
            return "Long solve time with hint dependency.";
        }
        if (s.daysSinceSolved() > 7) {
            return "Due for spaced repetition (" + s.daysSinceSolved() + " days ago).";
        }
        return "Moderate revision priority based on recency and difficulty.";
    }
}
