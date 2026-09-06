package openalice.server.health;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

    @GetMapping("/actuator/health")
    Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

