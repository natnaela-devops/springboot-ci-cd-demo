package com.example.demo;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/message")
    public Map<String, String> message() {
        return Map.of(
                "service", "springboot-gitops-demo",
                "status", "ready",
                "message", "Hello from the validated GitOps delivery lab");
    }
}
