package com.notify.backend.metrics;

import com.notify.backend.entity.ChannelType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMetrics {

    private static final String SENT    = "notifications.sent";
    private static final String FAILED  = "notifications.failed";
    private static final String RETRIED = "notifications.retried";
    private static final String DLQ     = "notifications.dlq";

    private final MeterRegistry meterRegistry;

    public void recordSent(ChannelType channel) {
        counter(SENT, channel).increment();
    }

    public void recordFailed(ChannelType channel) {
        counter(FAILED, channel).increment();
    }

    public void recordRetry(ChannelType channel) {
        counter(RETRIED, channel).increment();
    }

    public void recordDlq(ChannelType channel) {
        counter(DLQ, channel).increment();
    }

    private Counter counter(String name, ChannelType channel) {
        return Counter.builder(name)
                .tag("channel", channel != null ? channel.name() : "UNKNOWN")
                .register(meterRegistry);
    }
}