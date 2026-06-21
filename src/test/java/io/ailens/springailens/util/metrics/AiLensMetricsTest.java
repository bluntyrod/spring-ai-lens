package io.ailens.springailens.util.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.AnomalyType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AiLensMetricsTest {

    private MeterRegistry registry;
    private AiLensMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AiLensMetrics(registry);
    }

    private AiCallEvent event(long latencyMs, int promptTokens, int completionTokens,
                              AnomalyReport anomaly) {
        return new AiCallEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                "OllamaChatModel",
                "test prompt",
                "test response",
                latencyMs,
                promptTokens,
                completionTokens,
                anomaly,
                null
        );
    }

    @Test
    void recordsCallCount() {
        metrics.record(event(100, 10, 20, AnomalyReport.none()));
        metrics.record(event(200, 10, 20, AnomalyReport.none()));

        Counter counter = registry.find("spring.ai.lens.calls")
                .tag("model", "OllamaChatModel")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void recordsLatency() {
        metrics.record(event(150, 10, 20, AnomalyReport.none()));

        Timer timer = registry.find("spring.ai.lens.latency")
                .tag("model", "OllamaChatModel")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(150.0);
    }

    @Test
    void recordsPromptAndCompletionTokens() {
        metrics.record(event(100, 15, 30, AnomalyReport.none()));

        Counter promptCounter = registry.find("spring.ai.lens.tokens.prompt")
                .tag("model", "OllamaChatModel")
                .counter();
        Counter completionCounter = registry.find("spring.ai.lens.tokens.completion")
                .tag("model", "OllamaChatModel")
                .counter();

        assertThat(promptCounter).isNotNull();
        assertThat(promptCounter.count()).isEqualTo(15.0);
        assertThat(completionCounter).isNotNull();
        assertThat(completionCounter.count()).isEqualTo(30.0);
    }

    @Test
    void recordsAnomalyCount() {
        metrics.record(event(100, 10, 20,
                AnomalyReport.of(AnomalyType.LATENCY_SPIKE, "spike detected")));

        Counter anomalyCounter = registry.find("spring.ai.lens.anomalies")
                .tag("model", "OllamaChatModel")
                .tag("type", "LATENCY_SPIKE")
                .counter();

        assertThat(anomalyCounter).isNotNull();
        assertThat(anomalyCounter.count()).isEqualTo(1.0);
    }

    @Test
    void doesNotRecordAnomalyWhenNone() {
        metrics.record(event(100, 10, 20, AnomalyReport.none()));

        Counter anomalyCounter = registry.find("spring.ai.lens.anomalies")
                .tag("model", "OllamaChatModel")
                .counter();

        assertThat(anomalyCounter).isNull();
    }
}
