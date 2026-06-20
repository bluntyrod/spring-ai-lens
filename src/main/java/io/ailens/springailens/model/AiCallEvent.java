package io.ailens.springailens.model;

import java.time.Instant;

public record AiCallEvent(
        String id,
        Instant timestamp,
        String model,
        String prompt,
        String response,
        long latencyMs,
        int promptTokens,
        int completionTokens,
        AnomalyReport anomaly
) {}
