package prepwise.prepwise.ai.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class CompanyPatternWeights {

    private static final Map<String, Map<String, Integer>> WEIGHTS = new LinkedHashMap<>();

    static {
        WEIGHTS.put("GOOGLE", mapOf(
                "Graphs", 25, "DP", 20, "Trees", 20, "Arrays", 15, "Greedy", 10, "Others", 10));
        WEIGHTS.put("AMAZON", mapOf(
                "Heap", 20, "TopK", 20, "Greedy", 15, "Trees", 15, "Graphs", 15, "Others", 15));
        WEIGHTS.put("MICROSOFT", mapOf(
                "Arrays", 20, "Strings", 20, "Trees", 20, "DP", 15, "Graphs", 15, "Others", 10));
        WEIGHTS.put("ADOBE", mapOf(
                "DP", 20, "Greedy", 20, "BinarySearch", 15, "Trees", 15, "Graphs", 15, "Others", 15));
        WEIGHTS.put("META", mapOf(
                "Arrays", 25, "Graphs", 20, "DP", 20, "Strings", 15, "Trees", 10, "Others", 10));
        WEIGHTS.put("UBER", mapOf(
                "Graphs", 30, "BinarySearch", 20, "Arrays", 20, "Greedy", 15, "DP", 10, "Others", 5));
        WEIGHTS.put("STRIPE", mapOf(
                "Arrays", 25, "Strings", 25, "DP", 15, "Greedy", 15, "Trees", 10, "Others", 10));
        WEIGHTS.put("ATLASSIAN", mapOf(
                "Arrays", 20, "Strings", 20, "DP", 20, "Trees", 15, "Graphs", 15, "Others", 10));
    }

    private CompanyPatternWeights() {}

    public static Map<String, Integer> forCompany(String company) {
        if (company == null) return WEIGHTS.get("GOOGLE");
        String key = company.trim().toUpperCase();
        return WEIGHTS.getOrDefault(key, WEIGHTS.get("GOOGLE"));
    }

    public static boolean isKnown(String company) {
        return company != null && WEIGHTS.containsKey(company.trim().toUpperCase());
    }

    public static Set<String> knownCompanies() {
        return WEIGHTS.keySet();
    }

    private static Map<String, Integer> mapOf(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }
}
