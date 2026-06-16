package com.notify.backend.controller;

import com.notify.backend.dto.cohort.AddCohortUsersRequest;
import com.notify.backend.dto.cohort.AddCohortUsersResponse;
import com.notify.backend.dto.cohort.CohortResponse;
import com.notify.backend.dto.cohort.CreateCohortRequest;
import com.notify.backend.filter.UuidAuthFilter;
import com.notify.backend.service.CohortService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/cohorts")
@RequiredArgsConstructor
@Tag(name = "Cohorts", description = "Create and manage user cohorts")
public class CohortController {

    private final CohortService cohortService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new cohort for the authenticated client")
    public CohortResponse create(
            @Valid @RequestBody CreateCohortRequest request,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.info("POST /cohorts — clientId={}, name={}", clientId, request.getName());
        return cohortService.createCohort(request, clientId);
    }

    @GetMapping
    @Operation(summary = "List all cohorts for the authenticated client")
    public List<CohortResponse> list(HttpServletRequest httpRequest) {
        UUID clientId = extractClientId(httpRequest);
        log.debug("GET /cohorts — clientId={}", clientId);
        return cohortService.listCohorts(clientId);
    }

    @PostMapping("/{id}/users")
    @Operation(summary = "Add users to a cohort (bulk, with deduplication)")
    public AddCohortUsersResponse addUsers(
            @PathVariable Long id,
            @Valid @RequestBody AddCohortUsersRequest request,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.info("POST /cohorts/{}/users — clientId={}, count={}", id, clientId, request.getUsers().size());
        return cohortService.addUsers(id, clientId, request);
    }

    @DeleteMapping("/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a user from a cohort")
    public void removeUser(
            @PathVariable Long id,
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.info("DELETE /cohorts/{}/users/{} — clientId={}", id, userId, clientId);
        cohortService.removeUser(id, clientId, userId);
    }

    private UUID extractClientId(HttpServletRequest request) {
        return (UUID) request.getAttribute(UuidAuthFilter.CLIENT_ID_ATTR);
    }
}
