package prepwise.prepwise.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prepwise.prepwise.dto.ProblemAttemptRequest;
import prepwise.prepwise.dto.ProblemAttemptResponse;
import prepwise.prepwise.dto.ProblemResolveResponse;
import prepwise.prepwise.service.ProblemService;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping("/resolve")
    public ResponseEntity<ProblemResolveResponse> resolve(@RequestParam String url) {
        return ResponseEntity.ok(problemService.resolveUrl(url));
    }

    @PostMapping
    public ResponseEntity<ProblemAttemptResponse> logAttempt(@Valid @RequestBody ProblemAttemptRequest request) {
        return ResponseEntity.ok(problemService.logAttempt(request));
    }

    @GetMapping
    public ResponseEntity<List<ProblemAttemptResponse>> getAttempts(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(problemService.getAttempts(topic, pattern, difficulty, platform, search));
    }
}
