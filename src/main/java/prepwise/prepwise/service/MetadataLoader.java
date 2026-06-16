package prepwise.prepwise.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import prepwise.prepwise.entity.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@Getter
public class MetadataLoader {

    private final Map<String, ProblemMetadata> metadataMap = new HashMap<>();
    private final Map<String, List<SheetEntry>> sheetMap = new HashMap<>();
    private final Map<Platform, Map<String, String>> slugIndex = new EnumMap<>(Platform.class);

    public record SheetEntry(Platform platform, String slug) {}

    @PostConstruct
    public void load() {
        for (Platform p : Platform.values()) {
            if (p != Platform.MISCELLANEOUS) slugIndex.put(p, new HashMap<>());
        }
        loadPlatformCsv("datasets/leetcode.csv", Platform.LEETCODE);
        loadPlatformCsv("datasets/codeforces.csv", Platform.CODEFORCES);
        loadPlatformCsv("datasets/gfg.csv", Platform.GFG);
        loadPlatformCsv("datasets/hackerrank.csv", Platform.HACKERRANK);
        loadSheetCsv("datasets/striver_a2z.csv", "Striver A2Z");
        loadSheetCsv("datasets/codehelp_dsa.csv", "CodeHelp DSA");
        loadSheetCsv("datasets/neetcode_150.csv", "NeetCode 150");
        loadSheetCsv("datasets/blind_75.csv", "Blind 75");
        log.info("Loaded {} problem metadata entries and {} sheets", metadataMap.size(), sheetMap.size());
    }

    private void loadPlatformCsv(String path, Platform platform) {
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header != null && header.startsWith("\uFEFF")) header = header.substring(1);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length < 5) continue;
                String slug = normalizeSlug(parts[0]);
                String key = platform.name() + ":" + slug;
                metadataMap.put(key, new ProblemMetadata(
                        parts[1].trim(),
                        parseEnum(Difficulty.class, parts[2].trim(), Difficulty.UNKNOWN),
                        parseEnum(Topic.class, parts[3].trim(), Topic.UNKNOWN),
                        parseEnum(Pattern.class, parts[4].trim(), Pattern.UNKNOWN)
                ));
                slugIndex.get(platform).put(slug.toLowerCase(), slug);
            }
        } catch (Exception e) {
            log.error("Failed to load {}", path, e);
        }
    }

    private void loadSheetCsv(String path, String sheetName) {
        List<SheetEntry> entries = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length < 2) continue;
                Platform platform = parseEnum(Platform.class, parts[0].trim(), Platform.MISCELLANEOUS);
                entries.add(new SheetEntry(platform, normalizeSlug(parts[1])));
            }
        } catch (Exception e) {
            log.error("Failed to load sheet {}", path, e);
        }
        sheetMap.put(sheetName, entries);
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value, E fallback) {
        try {
            return Enum.valueOf(clazz, value.trim().toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String normalizeSlug(String slug) {
        if (slug == null) return "";
        return slug.trim().toLowerCase().replaceAll("/+$", "");
    }

    public Optional<ProblemMetadata> lookup(Platform platform, String slug) {
        return resolve(platform, slug);
    }

    public Optional<ProblemMetadata> resolve(Platform platform, String rawSlug) {
        if (platform == null || rawSlug == null || rawSlug.isBlank() || "unknown".equalsIgnoreCase(rawSlug)) {
            return Optional.empty();
        }

        String slug = normalizeSlug(rawSlug);

        // Direct lookup
        ProblemMetadata direct = metadataMap.get(platform.name() + ":" + slug);
        if (direct != null) return Optional.of(direct);

        // Case-insensitive via index
        Map<String, String> index = slugIndex.get(platform);
        if (index != null) {
            String canonical = index.get(slug.toLowerCase());
            if (canonical != null) {
                ProblemMetadata meta = metadataMap.get(platform.name() + ":" + canonical);
                if (meta != null) return Optional.of(meta);
            }
        }

        // Codeforces: normalize 4a -> 4A to match dataset slugs
        if (platform == Platform.CODEFORCES && slug.matches("\\d+[a-z]")) {
            String cfSlug = slug.substring(0, slug.length() - 1)
                    + Character.toUpperCase(slug.charAt(slug.length() - 1));
            ProblemMetadata meta = metadataMap.get(platform.name() + ":" + cfSlug);
            if (meta != null) return Optional.of(meta);
        }

        return Optional.empty();
    }

    public boolean isKnown(Platform platform, String slug) {
        return resolve(platform, slug).isPresent();
    }
}
