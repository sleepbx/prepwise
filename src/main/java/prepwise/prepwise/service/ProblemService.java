package prepwise.prepwise.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prepwise.prepwise.dto.ProblemAttemptRequest;
import prepwise.prepwise.dto.ProblemAttemptResponse;
import prepwise.prepwise.dto.ProblemResolveResponse;
import prepwise.prepwise.entity.*;
import prepwise.prepwise.repository.ProblemAttemptRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemAttemptRepository attemptRepository;
    private final MetadataLoader metadataLoader;
    private final UrlParserService urlParserService;
    private final StreakService streakService;
    private final CurrentUserService currentUserService;

    public ProblemResolveResponse resolveUrl(String problemUrl) {
        Platform platform = urlParserService.detectPlatform(problemUrl);
        String slug = urlParserService.extractSlug(problemUrl, platform);
        return buildResolveResponse(problemUrl, platform, slug);
    }

    @Transactional
    public ProblemAttemptResponse logAttempt(ProblemAttemptRequest request) {
        User user = currentUserService.getCurrentUser();
        Platform platform = urlParserService.detectPlatform(request.getProblemUrl());
        String slug = urlParserService.extractSlug(request.getProblemUrl(), platform);

        if ("unknown".equals(slug)) {
            throw new IllegalArgumentException(
                    "Could not parse problem from URL. Use a direct problem link, e.g. https://leetcode.com/problems/two-sum/");
        }

        ProblemMetadata meta = metadataLoader.resolve(platform, slug).orElseThrow(() ->
                new IllegalArgumentException(
                        "Problem not found in dataset (slug: " + slug + "). Try a problem from Blind 75, NeetCode 150, or our supported sheets."));

        ProblemAttempt attempt = ProblemAttempt.builder()
                .user(user)
                .problemUrl(normalizeStoredUrl(request.getProblemUrl()))
                .platform(platform)
                .slug(slug)
                .title(meta.getTitle())
                .difficulty(meta.getDifficulty())
                .topic(meta.getTopic())
                .pattern(meta.getPattern())
                .timeSpentMinutes(request.getTimeSpentMinutes())
                .hintUsed(request.isHintUsed())
                .solvedAt(LocalDateTime.now())
                .build();

        attempt = attemptRepository.save(attempt);
        streakService.updateStreak(user, attempt.getSolvedAt().toLocalDate());
        return toResponse(attempt);
    }

    @Transactional
    public List<ProblemAttemptResponse> getAttempts(String topic, String pattern,
                                                   String difficulty, String platform, String search) {
        User user = currentUserService.getCurrentUser();
        Specification<ProblemAttempt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user"), user));
            if (topic != null && !topic.isBlank()) {
                predicates.add(cb.equal(root.get("topic"), Topic.valueOf(topic)));
            }
            if (pattern != null && !pattern.isBlank()) {
                predicates.add(cb.equal(root.get("pattern"), Pattern.valueOf(pattern)));
            }
            if (difficulty != null && !difficulty.isBlank()) {
                predicates.add(cb.equal(root.get("difficulty"), Difficulty.valueOf(difficulty)));
            }
            if (platform != null && !platform.isBlank()) {
                predicates.add(cb.equal(root.get("platform"), Platform.valueOf(platform)));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("slug")), like)
                ));
            }
            query.orderBy(cb.desc(root.get("solvedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return attemptRepository.findAll(spec).stream()
                .map(this::rehydrateIfNeeded)
                .map(this::toResponse)
                .toList();
    }

    public List<ProblemAttempt> getUserAttempts(User user) {
        return attemptRepository.findByUserOrderBySolvedAtDesc(user).stream()
                .map(this::rehydrateIfNeeded)
                .toList();
    }

    private ProblemAttempt rehydrateIfNeeded(ProblemAttempt attempt) {
        if (!needsRehydrate(attempt)) return attempt;

        metadataLoader.resolve(attempt.getPlatform(), attempt.getSlug()).ifPresent(meta -> {
            attempt.setTitle(meta.getTitle());
            attempt.setDifficulty(meta.getDifficulty());
            attempt.setTopic(meta.getTopic());
            attempt.setPattern(meta.getPattern());
            attemptRepository.save(attempt);
        });
        return attempt;
    }

    private boolean needsRehydrate(ProblemAttempt attempt) {
        return "Unknown Problem".equals(attempt.getTitle())
                || attempt.getDifficulty() == Difficulty.UNKNOWN
                || attempt.getTopic() == Topic.UNKNOWN
                || attempt.getPattern() == Pattern.UNKNOWN;
    }

    private ProblemResolveResponse buildResolveResponse(String url, Platform platform, String slug) {
        var builder = ProblemResolveResponse.builder()
                .problemUrl(url)
                .platform(platform)
                .slug(slug);

        if ("unknown".equals(slug)) {
            return builder.found(false)
                    .title("Unrecognized URL")
                    .difficulty(Difficulty.UNKNOWN)
                    .topic(Topic.UNKNOWN)
                    .pattern(Pattern.UNKNOWN)
                    .message("Paste a direct problem link (e.g. leetcode.com/problems/two-sum/)")
                    .build();
        }

        return metadataLoader.resolve(platform, slug)
                .map(meta -> builder.found(true)
                        .title(meta.getTitle())
                        .difficulty(meta.getDifficulty())
                        .topic(meta.getTopic())
                        .pattern(meta.getPattern())
                        .message("Problem found in dataset")
                        .build())
                .orElseGet(() -> builder.found(false)
                        .title(humanizeSlug(slug))
                        .difficulty(Difficulty.UNKNOWN)
                        .topic(Topic.UNKNOWN)
                        .pattern(Pattern.UNKNOWN)
                        .message("Slug parsed as \"" + slug + "\" but not in dataset yet")
                        .build());
    }

    private String humanizeSlug(String slug) {
        return Arrays.stream(slug.split("-"))
                .map(s -> s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String normalizeStoredUrl(String url) {
        return url == null ? "" : url.trim();
    }

    public ProblemAttemptResponse toResponse(ProblemAttempt attempt) {
        return ProblemAttemptResponse.builder()
                .id(attempt.getId())
                .problemUrl(attempt.getProblemUrl())
                .platform(attempt.getPlatform())
                .slug(attempt.getSlug())
                .title(attempt.getTitle())
                .difficulty(attempt.getDifficulty())
                .topic(attempt.getTopic())
                .pattern(attempt.getPattern())
                .timeSpentMinutes(attempt.getTimeSpentMinutes())
                .hintUsed(attempt.isHintUsed())
                .solvedAt(attempt.getSolvedAt())
                .build();
    }
}
