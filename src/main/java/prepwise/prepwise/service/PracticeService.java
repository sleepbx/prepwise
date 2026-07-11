package prepwise.prepwise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prepwise.prepwise.dto.PracticeProblemDto;
import prepwise.prepwise.dto.PracticeQueueResponse;
import prepwise.prepwise.dto.RecommendedPatternDto;
import prepwise.prepwise.dto.ReviewQueueDto;
import prepwise.prepwise.entity.*;
import prepwise.prepwise.repository.ProblemAttemptRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private static final List<String> DAILY_SHEETS = List.of("Blind 75", "NeetCode 150", "Striver A2Z");
    private static final List<Integer> REVIEW_INTERVALS = List.of(1, 7, 14, 30);

    private final CurrentUserService currentUserService;
    private final ProblemService problemService;
    private final MetadataLoader metadataLoader;
    private final ProblemAttemptRepository attemptRepository;

    public PracticeProblemDto getQuestionOfTheDay() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        Set<String> solved = solvedKeys(attempts);
        LocalDate today = LocalDate.now();
        String sheet = DAILY_SHEETS.get(Math.floorMod(today.getDayOfYear(), DAILY_SHEETS.size()));
        List<MetadataLoader.SheetEntry> entries = metadataLoader.getSheetMap().getOrDefault(sheet, List.of());

        return pickFromEntries(entries, solved, List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD), Set.of(), today.getDayOfYear())
                .map(candidate -> toPracticeProblem(candidate, sheet, "Daily rotation across curated interview sheets."))
                .orElseGet(() -> fallbackProblem("Question of the day", attempts));
    }

    public PracticeQueueResponse getTodayQueue() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        Set<String> solved = solvedKeys(attempts);
        List<Pattern> weakPatterns = rankedWeakPatterns(attempts).stream().map(PatternScore::pattern).toList();

        PracticeProblemDto warmUp = pickRecommended(solved, weakPatterns, List.of(Difficulty.EASY), 11)
                .orElseGet(() -> fallbackProblem("Warm-up based on your weakest available pattern.", attempts));
        PracticeProblemDto core = pickRecommended(solved, weakPatterns, List.of(Difficulty.MEDIUM), 23)
                .orElseGet(() -> fallbackProblem("Core challenge based on your recent activity.", attempts));
        PracticeProblemDto stretch = pickRecommended(solved, weakPatterns, List.of(Difficulty.MEDIUM, Difficulty.HARD), 37)
                .orElseGet(() -> fallbackProblem("Stretch challenge for difficulty progression.", attempts));

        return PracticeQueueResponse.builder()
                .warmUp(warmUp)
                .coreChallenge(core)
                .stretchChallenge(stretch)
                .build();
    }

    public List<ReviewQueueDto> getReviewQueue() {
        User user = currentUserService.getCurrentUser();
        return problemService.getUserAttempts(user).stream()
                .map(this::toReviewItem)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ReviewQueueDto::getReviewDue))
                .limit(8)
                .toList();
    }

    @Transactional
    public ReviewQueueDto markReviewed(Long attemptId) {
        User user = currentUserService.getCurrentUser();
        ProblemAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Attempt not found");
        }
        attempt.setLastReviewedAt(LocalDateTime.now());
        return toReviewItem(attemptRepository.save(attempt));
    }

    public List<RecommendedPatternDto> getRecommendedPatterns() {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        return rankedWeakPatterns(attempts).stream()
                .limit(4)
                .map(score -> {
                    List<ProblemAttempt> patternAttempts = attempts.stream()
                            .filter(a -> a.getPattern() == score.pattern())
                            .toList();
                    LocalDate last = patternAttempts.stream()
                            .map(a -> a.getSolvedAt().toLocalDate())
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    int solved = patternAttempts.size();
                    int confidence = Math.max(5, Math.min(95, 100 - (int) Math.round(score.score())));
                    return RecommendedPatternDto.builder()
                            .pattern(score.pattern())
                            .label(formatPattern(score.pattern()))
                            .confidenceScore(confidence)
                            .lastPracticed(last)
                            .problemsSolved(solved)
                            .reason(reasonFor(score, solved, last))
                            .build();
                })
                .toList();
    }

    private Optional<PracticeProblemDto> pickRecommended(Set<String> solved, List<Pattern> weakPatterns,
                                                        List<Difficulty> difficulties, int salt) {
        LocalDate today = LocalDate.now();
        for (Pattern pattern : weakPatterns) {
            Optional<PracticeProblemDto> picked = DAILY_SHEETS.stream()
                    .map(sheet -> pickFromEntries(metadataLoader.getSheetMap().getOrDefault(sheet, List.of()),
                            solved, difficulties, Set.of(pattern), today.getDayOfYear() + salt))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .map(candidate -> toPracticeProblem(candidate, "Curated Sheets",
                            "Chosen from weak patterns, neglected topics, and difficulty progression."));
            if (picked.isPresent()) return picked;
        }
        return DAILY_SHEETS.stream()
                .map(sheet -> pickFromEntries(metadataLoader.getSheetMap().getOrDefault(sheet, List.of()),
                        solved, difficulties, Set.of(), today.getDayOfYear() + salt))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(candidate -> toPracticeProblem(candidate, "Curated Sheets",
                        "Chosen to keep today's practice balanced."));
    }

    private Optional<Candidate> pickFromEntries(List<MetadataLoader.SheetEntry> entries, Set<String> solved,
                                                List<Difficulty> difficulties, Set<Pattern> patterns, int seed) {
        List<Candidate> candidates = entries.stream()
                .filter(e -> !solved.contains(e.platform().name() + ":" + e.slug().toLowerCase()))
                .map(e -> metadataLoader.lookup(e.platform(), e.slug())
                        .map(meta -> new Candidate(e.platform(), e.slug(), meta)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(c -> difficulties.contains(c.meta().getDifficulty()))
                .filter(c -> patterns.isEmpty() || patterns.contains(c.meta().getPattern()))
                .toList();
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(Math.floorMod(seed, candidates.size())));
    }

    private ReviewQueueDto toReviewItem(ProblemAttempt attempt) {
        LocalDate base = attempt.getLastReviewedAt() == null
                ? attempt.getSolvedAt().toLocalDate()
                : attempt.getLastReviewedAt().toLocalDate();
        long age = ChronoUnit.DAYS.between(base, LocalDate.now());
        Integer interval = REVIEW_INTERVALS.stream().filter(i -> age >= i).reduce((a, b) -> b).orElse(null);
        if (interval == null) return null;
        LocalDate due = base.plusDays(interval);
        String priority = age >= 30 || attempt.isHintUsed() ? "HIGH" : age >= 14 ? "MEDIUM" : "LOW";
        return ReviewQueueDto.builder()
                .attemptId(attempt.getId())
                .problem(attempt.getTitle())
                .platform(attempt.getPlatform())
                .url(attempt.getProblemUrl())
                .solvedDate(attempt.getSolvedAt().toLocalDate())
                .reviewDue(due)
                .priority(priority)
                .build();
    }

    private List<PatternScore> rankedWeakPatterns(List<ProblemAttempt> attempts) {
        Map<Pattern, List<ProblemAttempt>> byPattern = attempts.stream()
                .filter(a -> a.getPattern() != Pattern.UNKNOWN)
                .collect(Collectors.groupingBy(ProblemAttempt::getPattern));
        List<PatternScore> scores = new ArrayList<>();
        for (Pattern pattern : Pattern.values()) {
            if (pattern == Pattern.UNKNOWN) continue;
            List<ProblemAttempt> patternAttempts = byPattern.getOrDefault(pattern, List.of());
            int count = patternAttempts.size();
            double lowSolveCount = Math.max(0, 8 - count) * 8;
            double hintDependency = patternAttempts.isEmpty() ? 12 :
                    patternAttempts.stream().filter(ProblemAttempt::isHintUsed).count() * 35.0 / count;
            double avgTime = patternAttempts.isEmpty() ? 25 :
                    patternAttempts.stream().mapToInt(ProblemAttempt::getTimeSpentMinutes).average().orElse(0);
            double solveTimePenalty = Math.min(30, Math.max(0, avgTime - 25) * 0.7);
            LocalDate last = patternAttempts.stream()
                    .map(a -> a.getSolvedAt().toLocalDate())
                    .max(Comparator.naturalOrder())
                    .orElse(LocalDate.now().minusDays(60));
            double inactivity = Math.min(35, ChronoUnit.DAYS.between(last, LocalDate.now()) * 1.1);
            scores.add(new PatternScore(pattern, lowSolveCount + hintDependency + solveTimePenalty + inactivity));
        }
        scores.sort((a, b) -> Double.compare(b.score(), a.score()));
        return scores;
    }

    private PracticeProblemDto toPracticeProblem(Candidate candidate, String sheet, String reason) {
        ProblemMetadata meta = candidate.meta();
        return PracticeProblemDto.builder()
                .title(meta.getTitle())
                .platform(candidate.platform())
                .slug(candidate.slug())
                .url(buildUrl(candidate.platform(), candidate.slug()))
                .difficulty(meta.getDifficulty())
                .topic(meta.getTopic())
                .pattern(meta.getPattern())
                .sheet(sheet)
                .estimatedSolveTimeMinutes(estimateMinutes(meta.getDifficulty()))
                .reason(reason)
                .build();
    }

    private PracticeProblemDto fallbackProblem(String reason, List<ProblemAttempt> attempts) {
        ProblemAttempt attempt = attempts.stream().findFirst().orElse(null);
        if (attempt == null) {
            return PracticeProblemDto.builder()
                    .title("Two Sum")
                    .platform(Platform.LEETCODE)
                    .slug("two-sum")
                    .url("https://leetcode.com/problems/two-sum/")
                    .difficulty(Difficulty.EASY)
                    .topic(Topic.ARRAY)
                    .pattern(Pattern.HASHING)
                    .sheet("Blind 75")
                    .estimatedSolveTimeMinutes(15)
                    .reason(reason)
                    .build();
        }
        return PracticeProblemDto.builder()
                .attemptId(attempt.getId())
                .title(attempt.getTitle())
                .platform(attempt.getPlatform())
                .slug(attempt.getSlug())
                .url(attempt.getProblemUrl())
                .difficulty(attempt.getDifficulty())
                .topic(attempt.getTopic())
                .pattern(attempt.getPattern())
                .sheet("Recent Attempt")
                .estimatedSolveTimeMinutes(estimateMinutes(attempt.getDifficulty()))
                .reason(reason)
                .build();
    }

    private Set<String> solvedKeys(List<ProblemAttempt> attempts) {
        return attempts.stream()
                .map(a -> a.getPlatform().name() + ":" + a.getSlug().toLowerCase())
                .collect(Collectors.toSet());
    }

    private int estimateMinutes(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 20;
            case MEDIUM -> 40;
            case HARD -> 65;
            default -> 30;
        };
    }

    private String buildUrl(Platform platform, String slug) {
        return switch (platform) {
            case LEETCODE -> "https://leetcode.com/problems/" + slug + "/";
            case CODEFORCES -> "https://codeforces.com/problemset/problem/" + slug;
            case GFG -> "https://www.geeksforgeeks.org/problems/" + slug + "/1";
            case HACKERRANK -> "https://www.hackerrank.com/challenges/" + slug;
            default -> "https://example.com/" + slug;
        };
    }

    private String reasonFor(PatternScore score, int solved, LocalDate last) {
        if (solved == 0) return "No solved problems yet in this pattern.";
        if (last != null && last.isBefore(LocalDate.now().minusDays(21))) return "This pattern has gone quiet recently.";
        return "Confidence is lower than your stronger patterns.";
    }

    private String formatPattern(Pattern pattern) {
        return Arrays.stream(pattern.name().replace("_PATTERN", "").split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private record Candidate(Platform platform, String slug, ProblemMetadata meta) {}
    private record PatternScore(Pattern pattern, double score) {}
}
