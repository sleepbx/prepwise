package prepwise.prepwise.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import prepwise.prepwise.entity.*;
import prepwise.prepwise.repository.ProblemAttemptRepository;
import prepwise.prepwise.repository.StreakRepository;
import prepwise.prepwise.repository.UserRepository;
import prepwise.prepwise.service.MetadataLoader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StreakRepository streakRepository;
    private final ProblemAttemptRepository attemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final MetadataLoader metadataLoader;

    @Value("${prepwise.seed.demo:true}")
    private boolean seedDemo;

    private static final String DEMO_EMAIL = "demo@prepwise.dev";
    private static final String DEMO_PASSWORD = "demo123";

    @Override
    public void run(String... args) {
        if (!seedDemo || userRepository.existsByEmail(DEMO_EMAIL)) return;

        User demo = userRepository.save(User.builder()
                .name("Alex Rivera")
                .email(DEMO_EMAIL)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .role("USER")
                .build());

        streakRepository.save(Streak.builder()
                .user(demo)
                .currentStreak(12)
                .longestStreak(21)
                .lastSolvedDate(LocalDate.now())
                .build());

        seedAttempts(demo);
        log.info("Demo user seeded: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private void seedAttempts(User user) {
        Random random = new Random(42);
        List<String> demoProblems = List.of(
                "LEETCODE:two-sum",
                "LEETCODE:best-time-to-buy-and-sell-stock",
                "LEETCODE:contains-duplicate",
                "LEETCODE:valid-anagram",
                "LEETCODE:group-anagrams",
                "LEETCODE:valid-parentheses",
                "LEETCODE:merge-intervals",
                "LEETCODE:3sum",
                "LEETCODE:container-with-most-water",
                "LEETCODE:longest-substring-without-repeating-characters",
                "LEETCODE:minimum-window-substring",
                "LEETCODE:climbing-stairs",
                "LEETCODE:coin-change",
                "LEETCODE:house-robber",
                "LEETCODE:longest-increasing-subsequence",
                "LEETCODE:number-of-islands",
                "LEETCODE:clone-graph",
                "LEETCODE:course-schedule",
                "LEETCODE:word-search",
                "LEETCODE:subsets",
                "LEETCODE:permutations",
                "LEETCODE:combination-sum",
                "LEETCODE:invert-binary-tree",
                "LEETCODE:maximum-depth-of-binary-tree",
                "LEETCODE:binary-tree-level-order-traversal",
                "LEETCODE:validate-binary-search-tree",
                "LEETCODE:kth-largest-element-in-an-array",
                "LEETCODE:top-k-frequent-elements",
                "LEETCODE:find-median-from-data-stream",
                "LEETCODE:rotting-oranges",
                "LEETCODE:word-ladder",
                "LEETCODE:network-delay-time",
                "LEETCODE:trapping-rain-water",
                "LEETCODE:sliding-window-maximum",
                "LEETCODE:binary-search",
                "LEETCODE:search-in-rotated-sorted-array",
                "LEETCODE:merge-k-sorted-lists",
                "LEETCODE:reverse-linked-list",
                "LEETCODE:linked-list-cycle",
                "LEETCODE:daily-temperatures",
                "LEETCODE:car-fleet",
                "LEETCODE:product-of-array-except-self",
                "LEETCODE:maximum-subarray",
                "LEETCODE:jump-game",
                "LEETCODE:unique-paths",
                "LEETCODE:decode-ways",
                "LEETCODE:palindromic-substrings",
                "LEETCODE:longest-palindromic-substring",
                "GFG:reverse-an-array",
                "GFG:kadanes-algorithm",
                "HACKERRANK:arrays-ds",
                "HACKERRANK:hash-tables-two-sum",
                "CODEFORCES:4A"
        );

        LocalDate today = LocalDate.now();
        for (int dayOffset = 45; dayOffset >= 0; dayOffset--) {
            LocalDate date = today.minusDays(dayOffset);
            int problemsToday = dayOffset % 7 == 0 ? 0 : random.nextInt(3) + 1;
            for (int p = 0; p < problemsToday; p++) {
                String key = demoProblems.get(random.nextInt(demoProblems.size()));
                String[] parts = key.split(":");
                Platform platform = Platform.valueOf(parts[0]);
                String slug = parts[1];
                var meta = metadataLoader.lookup(platform, slug).orElse(
                        new prepwise.prepwise.service.ProblemMetadata("Demo", Difficulty.MEDIUM, Topic.ARRAY, Pattern.HASHING));

                Difficulty diff = meta.getDifficulty();
                int baseTime = diff == Difficulty.EASY ? 15 : diff == Difficulty.MEDIUM ? 35 : 55;

                attemptRepository.save(ProblemAttempt.builder()
                        .user(user)
                        .problemUrl(buildUrl(platform, slug))
                        .platform(platform)
                        .slug(slug)
                        .title(meta.getTitle())
                        .difficulty(meta.getDifficulty())
                        .topic(meta.getTopic())
                        .pattern(meta.getPattern())
                        .timeSpentMinutes(baseTime + random.nextInt(20))
                        .hintUsed(random.nextDouble() < 0.18)
                        .solvedAt(LocalDateTime.of(date, java.time.LocalTime.of(10 + random.nextInt(10), random.nextInt(60))))
                        .build());
            }
        }
    }

    private String buildUrl(Platform platform, String slug) {
        return switch (platform) {
            case LEETCODE -> "https://leetcode.com/problems/" + slug + "/";
            case CODEFORCES -> "https://codeforces.com/problemset/problem/4/A";
            case GFG -> "https://www.geeksforgeeks.org/problems/" + slug + "/1";
            case HACKERRANK -> "https://www.hackerrank.com/challenges/" + slug;
            default -> "https://example.com/" + slug;
        };
    }
}
