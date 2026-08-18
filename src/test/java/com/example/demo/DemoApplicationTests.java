package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void controllerReturnsStableContract() {
        Map<String, String> response = new HelloController().message();

        assertThat(response)
                .containsEntry("service", "springboot-gitops-demo")
                .containsEntry("status", "ready");
    }
}
