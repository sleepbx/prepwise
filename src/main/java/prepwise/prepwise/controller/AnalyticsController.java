package prepwise.prepwise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prepwise.prepwise.dto.*;
import prepwise.prepwise.service.AnalyticsService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(analyticsService.getDashboard());
    }

    @GetMapping("/patterns")
    public ResponseEntity<List<DistributionDto>> patterns() {
        return ResponseEntity.ok(analyticsService.getPatternDistribution());
    }

    @GetMapping("/radar")
    public ResponseEntity<List<RadarDataDto>> radar() {
        return ResponseEntity.ok(analyticsService.getRadarData());
    }

    @GetMapping("/weak-zones")
    public ResponseEntity<List<WeakZoneDto>> weakZones() {
        return ResponseEntity.ok(analyticsService.getWeakZones());
    }

    @GetMapping("/time-analysis")
    public ResponseEntity<TimeAnalysisDto> timeAnalysis() {
        return ResponseEntity.ok(analyticsService.getTimeAnalysis());
    }

    @GetMapping("/improvement")
    public ResponseEntity<ImprovementDto> improvement() {
        return ResponseEntity.ok(analyticsService.getImprovement());
    }

    @GetMapping("/velocity")
    public ResponseEntity<List<DailyVelocityDto>> velocity() {
        return ResponseEntity.ok(analyticsService.getDailyVelocity());
    }

    @GetMapping("/difficulty")
    public ResponseEntity<List<DistributionDto>> difficulty() {
        return ResponseEntity.ok(analyticsService.getDifficultyDistribution());
    }

    @GetMapping("/topics")
    public ResponseEntity<List<DistributionDto>> topics() {
        return ResponseEntity.ok(analyticsService.getTopicDistribution());
    }

    @GetMapping("/hints")
    public ResponseEntity<List<HintAnalysisDto>> hints() {
        return ResponseEntity.ok(analyticsService.getHintAnalysis());
    }
}
