package com.notify.backend.controller;

import com.notify.backend.dto.dlq.DlqEventResponse;
import com.notify.backend.service.DlqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
@Tag(name = "DLQ", description = "Inspect and replay dead-letter notifications")
public class DlqController {

    private final DlqService dlqService;

    @GetMapping
    @Operation(summary = "List pending DLQ events (not yet replayed, newest first)")
    public Page<DlqEventResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /dlq — page={}, size={}", page, size);
        return dlqService.listPending(page, size);
    }

    @PostMapping("/{id}/reprocess")
    @Operation(summary = "Replay a DLQ event — republishes directly to the correct channel topic")
    public DlqEventResponse reprocess(@PathVariable Long id) {
        log.info("POST /dlq/{}/reprocess", id);
        return dlqService.reprocess(id);
    }
}