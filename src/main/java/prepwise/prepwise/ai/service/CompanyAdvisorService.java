package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.CompanyAdvisorRequest;
import prepwise.prepwise.ai.dto.CompanyAdvisorResponse;
import prepwise.prepwise.ai.dto.CompanyAdvisorResponse.HighROIItem;
import prepwise.prepwise.ai.dto.CompanyAdvisorResponse.TopicGap;
import prepwise.prepwise.ai.intelligence.AnalyticsFeatureExtractor;
import prepwise.prepwise.ai.intelligence.UserAnalyticsSnapshot;
import prepwise.prepwise.ai.llm.LLMService;
import prepwise.prepwise.ai.util.CompanyPatternWeights;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.service.CurrentUserService;
import prepwise.prepwise.service.ProblemService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CompanyAdvisorService {

    private final ProblemService problemService;
    private final CurrentUserService currentUserService;
    private final AnalyticsFeatureExtractor extractor;
    private final LLMService llmService;

    public CompanyAdvisorResponse advise(CompanyAdvisorRequest request) {
        User user = currentUserService.getCurrentUser();
        List<ProblemAttempt> attempts = problemService.getUserAttempts(user);
        UserAnalyticsSnapshot snapshot = extractor.extract(user, attempts);

        Map<String, Integer> weights = CompanyPatternWeights.forCompany(request.getCompany());

        // Build topic comparison data
        Map<String, TopicGap> topicComparison = new LinkedHashMap<>();
        List<Map.Entry<String, Double>> allGaps = new ArrayList<>();
        List<String> sufficient = new ArrayList<>();
        List<String> overPracticed = new ArrayList<>();

        for (var e : weights.entrySet()) {
            if ("Others".equals(e.getKey())) continue;
            int expected = e.getValue();
            int current = (int) extractor.topicCoveragePercent(snapshot, e.getKey());
            int gap = Math.max(0, expected - current);

            topicComparison.put(e.getKey(), TopicGap.builder()
                    .expected(expected)
                    .current(current)
                    .gap(gap)
                    .build());

            double weightedGap = expected * ((100 - current) / 100.0);
            allGaps.add(Map.entry(e.getKey(), weightedGap));

            if (current >= (int) (expected * 0.90)) sufficient.add(e.getKey());
            if (current > expected * 1.15) overPracticed.add(e.getKey());
        }
        allGaps.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> focusAreas = allGaps.stream()
                .filter(e -> !sufficient.contains(e.getKey()))
                .map(Map.Entry::getKey)
                .limit(3)
                .toList();

        Map<String, String> allocation = buildAllocation(focusAreas, allGaps);
        List<HighROIItem> highROI = buildHighROIItems(focusAreas, topicComparison);

        // Compact summary for LLM
        List<Map<String, Object>> gapList = focusAreas.stream()
                .map(topic -> {
                    TopicGap tg = topicComparison.get(topic);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("topic", topic);
                    m.put("expected", tg != null ? tg.getExpected() : 0);
                    m.put("current", tg != null ? tg.getCurrent() : 0);
                    m.put("gap", tg != null ? tg.getGap() : 0);
                    return m;
                }).toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("company", request.getCompany());
        summary.put("topGaps", gapList);
        summary.put("focusAreas", focusAreas);
        summary.put("sufficientTopics", sufficient);

        return CompanyAdvisorResponse.builder()
                .company(request.getCompany())
                .focusAreas(focusAreas)
                .weeklyAllocation(allocation)
                .explanation(llmService.generateCompanyAdvice(summary))
                .topicComparison(topicComparison)
                .topicsAlreadySufficient(sufficient)
                .topicsOverPracticed(overPracticed)
                .highROIItems(highROI)
                .build();
    }

    private Map<String, String> buildAllocation(List<String> focusAreas,
                                                  List<Map.Entry<String, Double>> gaps) {
        Map<String, String> allocation = new LinkedHashMap<>();
        double totalGap = gaps.stream()
                .filter(e -> focusAreas.contains(e.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .sum();
        for (String area : focusAreas) {
            double gap = gaps.stream().filter(e -> e.getKey().equals(area))
                    .mapToDouble(Map.Entry::getValue).sum();
            int percent = totalGap == 0 ? 100 / focusAreas.size()
                    : (int) Math.round((gap * 100.0) / totalGap);
            allocation.put(area, percent + "%");
        }
        return allocation;
    }

    private List<HighROIItem> buildHighROIItems(List<String> focusAreas,
                                                 Map<String, TopicGap> comparison) {
        List<HighROIItem> items = new ArrayList<>();
        for (int i = 0; i < focusAreas.size(); i++) {
            String topic = focusAreas.get(i);
            TopicGap tg = comparison.get(topic);
            int gap = tg != null ? tg.getGap() : 0;
            double gain = gap * 0.08;
            String gainStr = "+" + (int) gain + "–" + ((int) gain + 2) + " readiness points";
            String effort = i == 0 ? "HIGH" : i == 1 ? "MEDIUM" : "LOW";
            items.add(HighROIItem.builder().topic(topic).expectedGain(gainStr).effortLevel(effort).build());
        }
        return items;
    }
}
