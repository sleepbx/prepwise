package prepwise.prepwise.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight, public health probe. Lives outside /api/** so it is covered by
 * SecurityConfig's anyRequest().permitAll() and needs no JWT. Used by the
 * keep-warm GitHub Actions workflow to ping the free-tier backend on a schedule
 * so it does not spin down (avoiding the ~100s cold start on the next visit).
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
