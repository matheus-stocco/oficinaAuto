package com.oficina.saude;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Healthcheck do Dokploy: sem login, não expõe nenhum dado. */
@RestController
public class SaudeController {
    @GetMapping("/api/saude")
    public Map<String, String> saude() {
        return Map.of("status", "ok");
    }
}
