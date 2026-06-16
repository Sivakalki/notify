package com.notify.backend.controller;

import com.notify.backend.dto.client.RegisterClientRequest;
import com.notify.backend.dto.client.RegisterClientResponse;
import com.notify.backend.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client Registration", description = "Register a frontend client to obtain an API key")
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new frontend client",
               description = "Returns a one-time API key. Store it securely — it cannot be retrieved again.")
    public RegisterClientResponse register(
            @Valid @RequestBody RegisterClientRequest request,
            HttpServletRequest httpRequest) {

        String remoteIp = httpRequest.getRemoteAddr();
        log.info("Client registration request: name='{}' from ip={}", request.getName(), remoteIp);
        RegisterClientResponse response = clientService.register(request, remoteIp);
        log.info("Client registered successfully: clientId={}", response.getClientId());
        return response;
    }
}