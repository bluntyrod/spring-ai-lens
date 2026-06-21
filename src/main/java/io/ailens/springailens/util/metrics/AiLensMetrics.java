package io.ailens.springailens.util.metrics;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.ailens.springailens.model.AiCallEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class AiLensMetrics {

    private static final String PREFIX = "spring.ai.lens";

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Timer> latencyTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> promptTokenCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> completionTokenCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> callCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> anomalyCounters = new ConcurrentHashMap<>();

    public AiLensMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(AiCallEvent event) {
        String model = event.model();

        // call count
        callCounters.computeIfAbsent(model, m ->
                Counter.builder(PREFIX + ".calls")
                        .tag("model", m)
                        .description("Total LLM calls")
                        .register(registry)
        ).increment();

        // latency
        latencyTimers.computeIfAbsent(model, m ->
                Timer.builder(PREFIX + ".latency")
                        .tag("model", m)
                        .description("LLM call latency")
                        .register(registry)
        ).record(Duration.ofMillis(event.latencyMs()));

        // prompt tokens
        promptTokenCounters.computeIfAbsent(model, m ->
                Counter.builder(PREFIX + ".tokens.prompt")
                        .tag("model", m)
                        .description("Total prompt tokens used")
                        .register(registry)
        ).increment(event.promptTokens());

        // completion tokens
        completionTokenCounters.computeIfAbsent(model, m ->
                Counter.builder(PREFIX + ".tokens.completion")
                        .tag("model", m)
                        .description("Total completion tokens used")
                        .register(registry)
        ).increment(event.completionTokens());

        // anomaly count
        if (event.anomaly() != null && event.anomaly().hasAnomaly()) {
            String anomalyType = event.anomaly().type().name();
            anomalyCounters.computeIfAbsent(model + "." + anomalyType, key ->
                    Counter.builder(PREFIX + ".anomalies")
                            .tag("model", model)
                            .tag("type", anomalyType)
                            .description("Total anomalies detected")
                            .register(registry)
            ).increment();
        }
    }
}
