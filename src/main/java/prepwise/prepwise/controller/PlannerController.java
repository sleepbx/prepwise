package prepwise.prepwise.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prepwise.prepwise.ai.dto.StudyPlanRequest;
import prepwise.prepwise.ai.dto.StudyPlanResponse;
import prepwise.prepwise.ai.service.StudyPlanService;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final StudyPlanService studyPlanService;

    @PostMapping("/generate")
    public ResponseEntity<StudyPlanResponse> generate(@Valid @RequestBody StudyPlanRequest request) {
        return ResponseEntity.ok(studyPlanService.generate(request));
    }
}
