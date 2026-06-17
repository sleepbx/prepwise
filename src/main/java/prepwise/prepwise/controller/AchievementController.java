package prepwise.prepwise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prepwise.prepwise.dto.AchievementDto;
import prepwise.prepwise.service.AchievementService;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ResponseEntity<List<AchievementDto>> achievements() {
        return ResponseEntity.ok(achievementService.getAchievements());
    }
}
