package prepwise.prepwise.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prepwise.prepwise.ai.dto.*;
import prepwise.prepwise.ai.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final StudyPlanService studyPlanService;
    private final WeaknessAnalysisService weaknessAnalysisService;
    private final ReadinessService readinessService;
    private final RevisionPriorityService revisionPriorityService;
    private final CompanyAdvisorService companyAdvisorService;
    private final AnalyticsInsightService analyticsInsightService;
    private final DailyCoachService dailyCoachService;

    @PostMapping("/study-plan")
    public ResponseEntity<StudyPlanResponse> studyPlan(@Valid @RequestBody StudyPlanRequest request) {
        return ResponseEntity.ok(studyPlanService.generate(request));
    }

    @GetMapping("/weakness-analysis")
    public ResponseEntity<List<WeaknessAnalysisDto>> weaknessAnalysis() {
        return ResponseEntity.ok(weaknessAnalysisService.analyze());
    }

    @GetMapping("/readiness")
    public ResponseEntity<ReadinessResponse> readiness() {
        return ResponseEntity.ok(readinessService.calculate());
    }

    @GetMapping("/revision-priorities")
    public ResponseEntity<List<RevisionPriorityDto>> revisionPriorities() {
        return ResponseEntity.ok(revisionPriorityService.getPriorities());
    }

    @PostMapping("/company-advisor")
    public ResponseEntity<CompanyAdvisorResponse> companyAdvisor(@Valid @RequestBody CompanyAdvisorRequest request) {
        return ResponseEntity.ok(companyAdvisorService.advise(request));
    }

    @GetMapping("/insights")
    public ResponseEntity<AnalyticsInsightResponse> analyticsInsights() {
        return ResponseEntity.ok(analyticsInsightService.generateInsight());
    }

    @GetMapping("/daily-coach")
    public ResponseEntity<DailyCoachResponse> dailyCoach() {
        return ResponseEntity.ok(dailyCoachService.getBriefing());
    }

    @PostMapping("/daily-coach")
    public ResponseEntity<DailyCoachResponse> dailyCoach(@RequestBody DailyCoachRequest request) {
        return ResponseEntity.ok(dailyCoachService.getBriefing(request));
    }
}
