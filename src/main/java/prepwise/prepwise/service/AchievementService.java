package prepwise.prepwise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.dto.AchievementDto;
import prepwise.prepwise.entity.Pattern;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.Streak;
import prepwise.prepwise.entity.User;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final CurrentUserService currentUserService;
    private final ProblemService problemService;
    private final StreakService streakService;
    private final MetadataLoader metadataLoader;

    public List<AchievementDto> getAchievements() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        Streak streak = streakService.getStreak(user);

        int total = attempts.size();
        int graphSolved = countByPatterns(attempts, Set.of(Pattern.BFS, Pattern.DFS, Pattern.TOPOLOGICAL_SORT,
                Pattern.UNION_FIND, Pattern.SHORTEST_PATH));
        int dpSolved = countByPatterns(attempts, Set.of(Pattern.DP, Pattern.KNAPSACK, Pattern.LIS));
        int noHintDays = longestNoHintRun(attempts);

        return List.of(
                achievement("first_solve", "First Solve", "Log your first interview problem.", total, 1),
                achievement("7_day_streak", "7 Day Streak", "Solve on seven consecutive days.", streak.getLongestStreak(), 7),
                achievement("30_day_streak", "30 Day Streak", "Build a thirty day preparation streak.", streak.getLongestStreak(), 30),
                achievement("100_problems_solved", "100 Problems Solved", "Solve 100 total problems.", total, 100),
                achievement("graph_explorer", "Graph Explorer", "Solve 20 graph-pattern problems.", graphSolved, 20),
                achievement("dp_specialist", "DP Specialist", "Solve 20 dynamic programming problems.", dpSolved, 20),
                sheetAchievement("blind_75_completed", "Blind 75 Completed", "Complete Blind 75.", "Blind 75", attempts),
                sheetAchievement("neetcode_150_completed", "NeetCode 150 Completed", "Complete NeetCode 150.", "NeetCode 150", attempts),
                achievement("no_hint_week", "No Hint Week", "Solve for seven consecutive active days without hints.", noHintDays, 7)
        );
    }

    private AchievementDto sheetAchievement(String key, String name, String description, String sheet, List<ProblemAttempt> attempts) {
        List<MetadataLoader.SheetEntry> entries = metadataLoader.getSheetMap().getOrDefault(sheet, List.of());
        Set<String> solved = solvedKeys(attempts);
        int count = (int) entries.stream()
                .filter(e -> solved.contains(e.platform().name() + ":" + e.slug().toLowerCase()))
                .count();
        int target = Math.max(1, entries.size());
        return achievement(key, name, description, count, target);
    }

    private AchievementDto achievement(String key, String name, String description, int progress, int target) {
        int capped = Math.min(progress, target);
        return AchievementDto.builder()
                .key(key)
                .name(name)
                .description(description)
                .unlocked(progress >= target)
                .progress(capped)
                .target(target)
                .build();
    }

    private int countByPatterns(List<ProblemAttempt> attempts, Set<Pattern> patterns) {
        return (int) attempts.stream().filter(a -> patterns.contains(a.getPattern())).count();
    }

    private Set<String> solvedKeys(List<ProblemAttempt> attempts) {
        Set<String> keys = new HashSet<>();
        attempts.forEach(a -> keys.add(a.getPlatform().name() + ":" + a.getSlug().toLowerCase()));
        return keys;
    }

    private int longestNoHintRun(List<ProblemAttempt> attempts) {
        var byDate = attempts.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getSolvedAt().toLocalDate()));
        List<LocalDate> activeDates = byDate.keySet().stream().sorted(Comparator.naturalOrder()).toList();
        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate date : activeDates) {
            boolean noHints = byDate.get(date).stream().noneMatch(ProblemAttempt::isHintUsed);
            if (!noHints) {
                current = 0;
                previous = date;
                continue;
            }
            current = previous != null && date.equals(previous.plusDays(1)) ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = date;
        }
        return longest;
    }
}
