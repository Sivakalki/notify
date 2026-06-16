package com.notify.backend.dto.dashboard;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerLagEntry {

    private String groupId;
    private String topic;
    private int partition;
    private long lag;
}