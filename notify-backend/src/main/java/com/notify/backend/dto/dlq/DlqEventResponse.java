package com.notify.backend.dto.dlq;

import com.notify.backend.entity.DlqEvent;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqEventResponse {

    private Long id;
    private Long eventId;
    private String reason;
    private String payload;
    private Instant createdAt;
    private Instant replayedAt;

    public static DlqEventResponse from(DlqEvent dlq) {
        return DlqEventResponse.builder()
                .id(dlq.getId())
                .eventId(dlq.getEvent().getId())
                .reason(dlq.getReason())
                .payload(dlq.getPayload())
                .createdAt(dlq.getCreatedAt())
                .replayedAt(dlq.getReplayedAt())
                .build();
    }
}