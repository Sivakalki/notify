package com.notify.backend.service;

import com.notify.backend.dto.client.RegisterClientRequest;
import com.notify.backend.dto.client.RegisterClientResponse;

public interface ClientService {

    RegisterClientResponse register(RegisterClientRequest request, String remoteIp);
}