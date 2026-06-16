package com.notify.backend.service;

import com.notify.backend.dto.dashboard.ConsumerLagEntry;
import com.notify.backend.dto.dashboard.DashboardMetricsResponse;
import com.notify.backend.dto.dashboard.MetricsFilterRequest;

import java.util.List;

public interface DashboardService {

    DashboardMetricsResponse getMetrics(MetricsFilterRequest filter);

    List<ConsumerLagEntry> getConsumerLag();
}