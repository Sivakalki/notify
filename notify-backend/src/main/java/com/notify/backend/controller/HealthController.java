package com.notify.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Service liveness check")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Liveness check")
    public String health() {
        log.debug("Health check called");
        return "Notify is running";
    }
}