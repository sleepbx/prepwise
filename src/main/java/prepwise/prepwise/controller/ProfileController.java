package prepwise.prepwise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prepwise.prepwise.dto.ProfileResponse;
import prepwise.prepwise.service.AnalyticsService;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<ProfileResponse> profile() {
        return ResponseEntity.ok(analyticsService.getProfile());
    }
}
