package prepwise.prepwise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.dto.*;
import prepwise.prepwise.entity.*;
import prepwise.prepwise.service.MetadataLoader.SheetEntry;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<Pattern> RADAR_PATTERNS = List.of(
            Pattern.TWO_POINTER, Pattern.SLIDING_WINDOW, Pattern.DFS, Pattern.BFS,
            Pattern.KNAPSACK, Pattern.BINARY_SEARCH_PATTERN, Pattern.GREEDY_PATTERN, Pattern.BACKTRACKING_PATTERN
    );

    private final ProblemService problemService;
    private final StreakService streakService;
    private final MetadataLoader metadataLoader;
    private final CurrentUserService currentUserService;

    public DashboardResponse getDashboard() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        Streak streak = streakService.getStreak(user);

        int totalSolved = attempts.size();
        int timeInvested = attempts.stream().mapToInt(ProblemAttempt::getTimeSpentMinutes).sum();
        double avgTime = totalSolved == 0 ? 0 : (double) timeInvested / totalSolved;
        long hintCount = attempts.stream().filter(ProblemAttempt::isHintUsed).count();
        double hintPercent = totalSolved == 0 ? 0 : (hintCount * 100.0) / totalSolved;

        LocalDate weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        int sprintProgress = (int) attempts.stream()
                .filter(a -> !a.getSolvedAt().toLocalDate().isBefore(weekStart))
                .count();

        Set<String> solvedKeys = attempts.stream()
                .map(a -> a.getPlatform().name() + ":" + a.getSlug().toLowerCase())
                .collect(Collectors.toSet());

        return DashboardResponse.builder()
                .currentStreak(streak.getCurrentStreak())
                .sprintProgress(sprintProgress)
                .totalSolved(totalSolved)
                .problemsSolved(totalSolved)
                .timeInvestedMinutes(timeInvested)
                .averageSolveTime(round(avgTime))
                .hintUsagePercent(round(hintPercent))
                .sheetProgress(buildSheetProgress(solvedKeys, attempts))
                .topPatterns(buildTopPatterns(attempts, 4))
                .recentAttempts(attempts.stream().limit(5).map(problemService::toResponse).toList())
                .build();
    }

    public List<DailyVelocityDto> getDailyVelocity() {
        List<ProblemAttempt> attempts = problemService.getUserAttempts(currentUserService.getCurrentUser());
        LocalDate start = LocalDate.now().minusDays(29);
        Map<LocalDate, Long> byDate = attempts.stream()
                .filter(a -> !a.getSolvedAt().toLocalDate().isBefore(start))
                .collect(Collectors.groupingBy(a -> a.getSolvedAt().toLocalDate(), Collectors.counting()));

        List<DailyVelocityDto> result = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = start.plusDays(i);
            result.add(DailyVelocityDto.builder()
                    .date(date.toString())
                    .count(byDate.getOrDefault(date, 0L).intValue())
                    .build());
        }
        return result;
    }

    public List<RadarDataDto> getRadarData() {
        List<ProblemAttempt> attempts = problemService.getUserAttempts(currentUserService.getCurrentUser());
        Map<Pattern, Long> counts = attempts.stream()
                .collect(Collectors.groupingBy(ProblemAttempt::getPattern, Collectors.counting()));

        long max = RADAR_PATTERNS.stream().mapToLong(p -> counts.getOrDefault(p, 0L)).max().orElse(0);
        if (max == 0) max = 1;

        long finalMax = max;
        return RADAR_PATTERNS.stream()
                .map(p -> {
                    int count = counts.getOrDefault(p, 0L).intValue();
                    double confidence = (count * 100.0) / finalMax;
                    return RadarDataDto.builder()
                            .pattern(formatPattern(p))
                            .confidence(round(confidence))
                            .count(count)
                            .build();
                })
                .toList();
    }

    public List<DistributionDto> getDifficultyDistribution() {
        return buildDistribution(problemService.getUserAttempts(currentUserService.getCurrentUser()),
                a -> a.getDifficulty().name());
    }

    public List<DistributionDto> getTopicDistribution() {
        return buildDistribution(problemService.getUserAttempts(currentUserService.getCurrentUser()),
                a -> formatEnum(a.getTopic().name()));
    }

    public List<DistributionDto> getPatternDistribution() {
        return buildDistribution(problemService.getUserAttempts(currentUserService.getCurrentUser()),
                a -> formatPattern(a.getPattern()));
    }

    public TimeAnalysisDto getTimeAnalysis() {
        List<ProblemAttempt> attempts = problemService.getUserAttempts(currentUserService.getCurrentUser());
        return TimeAnalysisDto.builder()
                .easy(avgTimeForDifficulty(attempts, Difficulty.EASY))
                .medium(avgTimeForDifficulty(attempts, Difficulty.MEDIUM))
                .hard(avgTimeForDifficulty(attempts, Difficulty.HARD))
                .build();
    }

    public List<HintAnalysisDto> getHintAnalysis() {
        List<ProblemAttempt> attempts = problemService.getUserAttempts(currentUserService.getCurrentUser());
        return List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD).stream()
                .map(d -> {
                    List<ProblemAttempt> filtered = attempts.stream()
                            .filter(a -> a.getDifficulty() == d).toList();
                    int total = filtered.size();
                    long hints = filtered.stream().filter(ProblemAttempt::isHintUsed).count();
                    double pct = total == 0 ? 0 : (hints * 100.0) / total;
                    return HintAnalysisDto.builder()
                            .difficulty(d.name())
                            .hintPercent(round(pct))
                            .total(total)
                            .build();
                })
                .toList();
    }

    public ImprovementDto getImprovement() {
        List<ProblemAttempt> attempts = problemService.getUserAttempts(currentUserService.getCurrentUser());
        LocalDate today = LocalDate.now();
        LocalDate last7Start = today.minusDays(6);
        LocalDate prev7Start = today.minusDays(13);
        LocalDate prev7End = today.minusDays(7);

        List<ProblemAttempt> last7 = filterByDateRange(attempts, last7Start, today);
        List<ProblemAttempt> prev7 = filterByDateRange(attempts, prev7Start, prev7End);

        double last7Avg = avgTime(last7);
        double prev7Avg = avgTime(prev7);
        double last7Hint = hintPercent(last7);
        double prev7Hint = hintPercent(prev7);

        return ImprovementDto.builder()
                .last7Problems(last7.size())
                .prev7Problems(prev7.size())
                .problemsSolvedDiff(last7.size() - prev7.size())
                .last7AvgTime(round(last7Avg))
                .prev7AvgTime(round(prev7Avg))
                .avgSolveTimeDiff(round(last7Avg - prev7Avg))
                .last7HintPercent(round(last7Hint))
                .prev7HintPercent(round(prev7Hint))
                .hintUsageDiff(round(last7Hint - prev7Hint))
                .build();
    }

    public List<WeakZoneDto> getWeakZones() {
        List<ProblemAttempt> attempts = problemService.getUserAttempts(currentUserService.getCurrentUser());
        LocalDate cutoff = LocalDate.now().minusDays(30);
        Set<Pattern> practiced = attempts.stream()
                .filter(a -> !a.getSolvedAt().toLocalDate().isBefore(cutoff))
                .map(ProblemAttempt::getPattern)
                .collect(Collectors.toSet());
        Set<Pattern> everSolved = attempts.stream()
                .map(ProblemAttempt::getPattern)
                .collect(Collectors.toSet());

        List<WeakZoneDto> weak = new ArrayList<>();
        for (Pattern p : Pattern.values()) {
            if (p == Pattern.UNKNOWN) continue;
            if (!everSolved.contains(p)) {
                weak.add(WeakZoneDto.builder()
                        .pattern(p)
                        .label(formatPattern(p))
                        .reason("Never solved")
                        .build());
            } else if (!practiced.contains(p)) {
                weak.add(WeakZoneDto.builder()
                        .pattern(p)
                        .label(formatPattern(p))
                        .reason("Not practiced in last 30 days")
                        .build());
            }
        }
        return weak.stream().limit(8).toList();
    }

    public ProfileResponse getProfile() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        Streak streak = streakService.getStreak(user);

        Map<Platform, Long> platformCounts = attempts.stream()
                .collect(Collectors.groupingBy(ProblemAttempt::getPlatform, Collectors.counting()));
        Map<Pattern, Long> patternCounts = attempts.stream()
                .collect(Collectors.groupingBy(ProblemAttempt::getPattern, Collectors.counting()));

        String favoritePlatform = platformCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> formatEnum(e.getKey().name()))
                .orElse("—");

        String strongest = patternCounts.entrySet().stream()
                .filter(e -> e.getKey() != Pattern.UNKNOWN)
                .max(Map.Entry.comparingByValue())
                .map(e -> formatPattern(e.getKey()))
                .orElse("—");

        List<WeakZoneDto> weakZones = getWeakZones();
        String weakest = weakZones.isEmpty() ? "—" : weakZones.get(0).getLabel();

        return ProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .initials(getInitials(user.getName()))
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .totalSolved(attempts.size())
                .totalTimeMinutes(attempts.stream().mapToInt(ProblemAttempt::getTimeSpentMinutes).sum())
                .favoritePlatform(favoritePlatform)
                .strongestPattern(strongest)
                .weakestPattern(weakest)
                .heatmap(buildHeatmap(attempts))
                .build();
    }

    private List<ProfileResponse.HeatmapDayDto> buildHeatmap(List<ProblemAttempt> attempts) {
        LocalDate start = LocalDate.now().minusWeeks(6).with(java.time.DayOfWeek.MONDAY);
        LocalDate end = LocalDate.now();
        Map<LocalDate, List<ProblemAttempt>> byDate = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getSolvedAt().toLocalDate()));

        List<ProfileResponse.HeatmapDayDto> heatmap = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<ProblemAttempt> dayAttempts = byDate.getOrDefault(d, List.of());
            int count = dayAttempts.size();
            int minutes = dayAttempts.stream().mapToInt(ProblemAttempt::getTimeSpentMinutes).sum();
            int level = count == 0 ? 0 : count <= 1 ? 1 : count <= 3 ? 2 : count <= 5 ? 3 : 4;
            heatmap.add(ProfileResponse.HeatmapDayDto.builder()
                    .date(d.toString())
                    .problemsSolved(count)
                    .minutesInvested(minutes)
                    .level(level)
                    .build());
        }
        return heatmap;
    }

    private List<DashboardResponse.SheetProgressDto> buildSheetProgress(Set<String> solvedKeys, List<ProblemAttempt> attempts) {
        Set<Pattern> solvedPatterns = attempts.stream().map(ProblemAttempt::getPattern).collect(Collectors.toSet());
        return metadataLoader.getSheetMap().entrySet().stream()
                .map(entry -> {
                    List<SheetEntry> sheetProblems = entry.getValue();
                    int total = sheetProblems.size();
                    long solved = sheetProblems.stream()
                            .filter(e -> solvedKeys.contains(e.platform().name() + ":" + e.slug().toLowerCase()))
                            .count();
                    Set<Pattern> sheetPatterns = sheetProblems.stream()
                            .map(e -> metadataLoader.lookup(e.platform(), e.slug())
                                    .map(ProblemMetadata::getPattern).orElse(Pattern.UNKNOWN))
                            .filter(p -> p != Pattern.UNKNOWN)
                            .collect(Collectors.toSet());
                    int patternsCovered = (int) sheetPatterns.stream().filter(solvedPatterns::contains).count();
                    double pct = total == 0 ? 0 : (solved * 100.0) / total;
                    return DashboardResponse.SheetProgressDto.builder()
                            .name(entry.getKey())
                            .solved((int) solved)
                            .total(total)
                            .percentage(round(pct))
                            .patternsCovered(patternsCovered)
                            .build();
                })
                .toList();
    }

    private List<WeakZoneDto> getWeakZonesFromAttempts(List<ProblemAttempt> attempts) {
        LocalDate cutoff = LocalDate.now().minusDays(30);
        Set<Pattern> practiced = attempts.stream()
                .filter(a -> !a.getSolvedAt().toLocalDate().isBefore(cutoff))
                .map(ProblemAttempt::getPattern).collect(Collectors.toSet());
        Set<Pattern> everSolved = attempts.stream().map(ProblemAttempt::getPattern).collect(Collectors.toSet());

        List<WeakZoneDto> weak = new ArrayList<>();
        for (Pattern p : Pattern.values()) {
            if (p == Pattern.UNKNOWN) continue;
            if (!everSolved.contains(p)) {
                weak.add(WeakZoneDto.builder().pattern(p).label(formatPattern(p)).reason("Never solved").build());
            } else if (!practiced.contains(p)) {
                weak.add(WeakZoneDto.builder().pattern(p).label(formatPattern(p)).reason("Not practiced in last 30 days").build());
            }
        }
        return weak;
    }

    private List<DashboardResponse.PatternCountDto> buildTopPatterns(List<ProblemAttempt> attempts, int limit) {
        return attempts.stream()
                .collect(Collectors.groupingBy(ProblemAttempt::getPattern, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getKey() != Pattern.UNKNOWN)
                .sorted(Map.Entry.<Pattern, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> DashboardResponse.PatternCountDto.builder()
                        .pattern(e.getKey())
                        .label(formatPattern(e.getKey()))
                        .count(e.getValue().intValue())
                        .build())
                .toList();
    }

    private List<DistributionDto> buildDistribution(List<ProblemAttempt> attempts,
                                                     java.util.function.Function<ProblemAttempt, String> labelFn) {
        Map<String, Long> counts = attempts.stream()
                .collect(Collectors.groupingBy(labelFn, Collectors.counting()));
        int total = attempts.size();
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> DistributionDto.builder()
                        .label(e.getKey())
                        .count(e.getValue().intValue())
                        .percentage(total == 0 ? 0 : round(e.getValue() * 100.0 / total))
                        .build())
                .toList();
    }

    private double avgTimeForDifficulty(List<ProblemAttempt> attempts, Difficulty d) {
        List<ProblemAttempt> filtered = attempts.stream().filter(a -> a.getDifficulty() == d).toList();
        return round(avgTime(filtered));
    }

    private double avgTime(List<ProblemAttempt> attempts) {
        if (attempts.isEmpty()) return 0;
        return (double) attempts.stream().mapToInt(ProblemAttempt::getTimeSpentMinutes).sum() / attempts.size();
    }

    private double hintPercent(List<ProblemAttempt> attempts) {
        if (attempts.isEmpty()) return 0;
        long hints = attempts.stream().filter(ProblemAttempt::isHintUsed).count();
        return (hints * 100.0) / attempts.size();
    }

    private List<ProblemAttempt> filterByDateRange(List<ProblemAttempt> attempts, LocalDate start, LocalDate end) {
        return attempts.stream()
                .filter(a -> {
                    LocalDate d = a.getSolvedAt().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .toList();
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String formatPattern(Pattern p) {
        return formatEnum(p.name().replace("_PATTERN", ""));
    }

    private String formatEnum(String name) {
        return Arrays.stream(name.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
