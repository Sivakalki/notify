package com.notify.backend.kafka;

/**
 * Central registry of Kafka topic names.
 * Use these constants in @KafkaListener and NotificationProducer
 * so topic names are never duplicated as string literals.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // API routes directly to one of these three based on channel
    public static final String EMAIL_NOTIFICATION      = "email-notification";
    public static final String SMS_NOTIFICATION        = "sms-notification";
    public static final String IN_APP_NOTIFICATION     = "in-app-notification";

    // Channel consumers publish here after every attempt (SENT or DLQ after max retries)
    public static final String NOTIFICATION_STATUS     = "notification-status";

    // Channel consumers publish here on failure; RetryConsumer handles backoff
    public static final String RETRY_NOTIFICATION      = "retry-notification";
}