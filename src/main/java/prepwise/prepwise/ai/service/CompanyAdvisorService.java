package prepwise.prepwise.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prepwise.prepwise.ai.dto.CompanyAdvisorRequest;
import prepwise.prepwise.ai.dto.CompanyAdvisorResponse;
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
        List<Map.Entry<String, Double>> gaps = new ArrayList<>();
        for (var e : weights.entrySet()) {
            if ("Others".equals(e.getKey())) continue;
            double coverage = extractor.topicCoveragePercent(snapshot, e.getKey());
            double weightedGap = e.getValue() * ((100 - coverage) / 100.0);
            gaps.add(Map.entry(e.getKey(), weightedGap));
        }
        gaps.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> focusAreas = gaps.stream().map(Map.Entry::getKey).limit(3).toList();
        Map<String, String> allocation = new LinkedHashMap<>();
        double totalGap = gaps.stream().limit(3).mapToDouble(Map.Entry::getValue).sum();
        for (int i = 0; i < focusAreas.size(); i++) {
            double gap = gaps.get(i).getValue();
            int percent = totalGap == 0 ? 100 / focusAreas.size() : (int) Math.round((gap * 100.0) / totalGap);
            allocation.put(focusAreas.get(i), percent + "%");
        }

        Map<String, Object> summary = Map.of(
                "company", request.getCompany(),
                "companyExpectations", weights,
                "focusAreas", focusAreas,
                "weeklyAllocation", allocation,
                "weakTopics", focusAreas
        );

        return CompanyAdvisorResponse.builder()
                .company(request.getCompany())
                .focusAreas(focusAreas)
                .weeklyAllocation(allocation)
                .explanation(llmService.generateCompanyAdvice(summary))
                .build();
    }
}
