package prepwise.prepwise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prepwise.prepwise.dto.PracticeProblemDto;
import prepwise.prepwise.dto.PracticeQueueResponse;
import prepwise.prepwise.dto.RecommendedPatternDto;
import prepwise.prepwise.dto.ReviewQueueDto;
import prepwise.prepwise.service.PracticeService;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/question-of-day")
    public ResponseEntity<PracticeProblemDto> questionOfDay() {
        return ResponseEntity.ok(practiceService.getQuestionOfTheDay());
    }

    @GetMapping("/today")
    public ResponseEntity<PracticeQueueResponse> today() {
        return ResponseEntity.ok(practiceService.getTodayQueue());
    }

    @GetMapping("/review-queue")
    public ResponseEntity<List<ReviewQueueDto>> reviewQueue() {
        return ResponseEntity.ok(practiceService.getReviewQueue());
    }

    @PostMapping("/reviewed/{attemptId}")
    public ResponseEntity<ReviewQueueDto> reviewed(@PathVariable Long attemptId) {
        return ResponseEntity.ok(practiceService.markReviewed(attemptId));
    }

    @GetMapping("/recommended-patterns")
    public ResponseEntity<List<RecommendedPatternDto>> recommendedPatterns() {
        return ResponseEntity.ok(practiceService.getRecommendedPatterns());
    }
}
