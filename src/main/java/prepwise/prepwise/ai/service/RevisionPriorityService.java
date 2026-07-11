package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.RevisionPriorityDto;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.util.IntelligenceScoring;
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
                        .urgencyCategory(IntelligenceScoring.urgencyCategory(s.priorityScore(), s.daysSinceSolved()))
                        .difficulty(s.difficulty().name())
                        .daysSinceSolved(s.daysSinceSolved())
                        .revisionCount(s.revisionCount())
                        .confidence(s.hintUsed() ? "LOW" : s.revisionCount() > 2 ? "HIGH" : "MEDIUM")
                        .build())
                .toList();
    }

    private String buildReason(AnalyticsFeatureExtractor.ProblemRevisionStats s) {
        if (s.revisionCount() == 0 && s.daysSinceSolved() > 14) {
            return "Never revised — " + s.daysSinceSolved() + " days since first solve. Retention likely low.";
        }
        if (s.hintUsed() && s.timeSpentMinutes() > 40) {
            return "Solved with hint in " + s.timeSpentMinutes() + "m — indicates understanding gap, not just time pressure.";
        }
        if (s.hintUsed()) {
            return "Hint was used during solve — revisit to confirm independent understanding.";
        }
        if (s.daysSinceSolved() > 7) {
            return "Spaced repetition due — last solved " + s.daysSinceSolved() + " days ago.";
        }
        return "Low-priority maintenance revision based on recency and difficulty.";
    }
}
