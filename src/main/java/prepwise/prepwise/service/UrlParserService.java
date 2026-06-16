package prepwise.prepwise.service;

import org.springframework.stereotype.Component;
import prepwise.prepwise.entity.Platform;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UrlParserService {

    private static final Pattern LEETCODE_SLUG = Pattern.compile(
            "leetcode\\.(?:com|cn)/problems?/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODEFORCES_PROBLEMSET = Pattern.compile(
            "codeforces\\.com/problemset/problem/(\\d+)/([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODEFORCES_CONTEST = Pattern.compile(
            "codeforces\\.com/contest/(\\d+)/problem/([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODEFORCES_LEGACY = Pattern.compile(
            "codeforces\\.com/(?:contest/\\d+/)?problem/([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HACKERRANK_SLUG = Pattern.compile(
            "hackerrank\\.com/(?:[^/]+/)*challenges/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GFG_PROBLEMS = Pattern.compile(
            "geeksforgeeks\\.org/problems/([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);

    public Platform detectPlatform(String url) {
        String lower = normalizeUrl(url);
        if (lower.contains("leetcode.com") || lower.contains("leetcode.cn")) return Platform.LEETCODE;
        if (lower.contains("codeforces.com")) return Platform.CODEFORCES;
        if (lower.contains("geeksforgeeks.org")) return Platform.GFG;
        if (lower.contains("hackerrank.com")) return Platform.HACKERRANK;
        return Platform.MISCELLANEOUS;
    }

    public String extractSlug(String url, Platform platform) {
        String normalized = normalizeUrl(url);
        try {
            return switch (platform) {
                case LEETCODE -> extractLeetcodeSlug(normalized);
                case CODEFORCES -> extractCodeforcesSlug(normalized);
                case GFG -> extractGfgSlug(normalized);
                case HACKERRANK -> match(HACKERRANK_SLUG, normalized);
                default -> extractFallbackSlug(normalized);
            };
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) return "unknown";
        return slug.trim().toLowerCase()
                .replaceAll("/+$", "")
                .replaceAll("^/+", "");
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (!trimmed.matches("(?i)^https?://.*")) {
            trimmed = "https://" + trimmed;
        }
        try {
            trimmed = URLDecoder.decode(trimmed, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        return trimmed;
    }

    private String extractLeetcodeSlug(String url) {
        Matcher m = LEETCODE_SLUG.matcher(url);
        if (m.find()) return normalizeSlug(m.group(1));
        return "unknown";
    }

    private String extractCodeforcesSlug(String url) {
        Matcher problemset = CODEFORCES_PROBLEMSET.matcher(url);
        if (problemset.find()) {
            return normalizeSlug(problemset.group(1) + problemset.group(2));
        }
        Matcher contest = CODEFORCES_CONTEST.matcher(url);
        if (contest.find()) {
            return normalizeSlug(contest.group(1) + contest.group(2));
        }
        Matcher legacy = CODEFORCES_LEGACY.matcher(url);
        if (legacy.find()) return normalizeSlug(legacy.group(1));
        return "unknown";
    }

    private String extractGfgSlug(String url) {
        Matcher m = GFG_PROBLEMS.matcher(url);
        if (m.find()) return normalizeSlug(m.group(1));

        // Fallback: walk path segments, skip numeric-only trailing parts like /1
        try {
            var uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path != null) {
                String[] segments = path.split("/");
                for (int i = segments.length - 1; i >= 0; i--) {
                    String seg = segments[i].trim();
                    if (seg.isBlank() || seg.matches("\\d+")) continue;
                    if ("problems".equalsIgnoreCase(seg)) continue;
                    return normalizeSlug(seg);
                }
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String extractFallbackSlug(String url) {
        try {
            var uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path != null) {
                String[] segments = path.split("/");
                for (int i = segments.length - 1; i >= 0; i--) {
                    String seg = segments[i].trim();
                    if (!seg.isBlank() && !seg.matches("\\d+")) {
                        return normalizeSlug(seg);
                    }
                }
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String match(Pattern pattern, String url) {
        Matcher m = pattern.matcher(url);
        if (m.find()) return normalizeSlug(m.group(1));
        return "unknown";
    }
}
