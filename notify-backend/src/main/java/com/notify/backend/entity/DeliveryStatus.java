package com.notify.backend.entity;

public enum DeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    RETRYING,
    DLQ
}