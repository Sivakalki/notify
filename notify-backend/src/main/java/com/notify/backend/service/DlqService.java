package com.notify.backend.service;

import com.notify.backend.dto.dlq.DlqEventResponse;
import org.springframework.data.domain.Page;

public interface DlqService {

    Page<DlqEventResponse> listPending(int page, int size);

    DlqEventResponse reprocess(Long dlqEventId);
}