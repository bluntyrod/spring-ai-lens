package io.ailens.springailens.anomaly;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.AnomalyType;
import io.ailens.springailens.store.RingBufferEventStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectorTest {

    private AiCallEvent event(long latencyMs, int promptTokens, int completionTokens) {
        return new AiCallEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                "OllamaChatModel",
                "test prompt",
                "test response",
                latencyMs,
                promptTokens,
                completionTokens,
                AnomalyReport.none()
        );
    }

    @Test
    void noAnomalyWhenInsufficientHistory() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AnomalyDetector detector = new AnomalyDetector(store);

        store.add(event(100, 10, 10));
        store.add(event(100, 10, 10));

        AnomalyReport report = detector.analyze(event(500, 50, 50));

        assertThat(report.hasAnomaly()).isFalse();
    }

    @Test
    void detectsLatencySpike() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AnomalyDetector detector = new AnomalyDetector(store);

        store.add(event(100, 10, 10));
        store.add(event(100, 10, 10));
        store.add(event(100, 10, 10));

        AnomalyReport report = detector.analyze(event(500, 10, 10));

        assertThat(report.hasAnomaly()).isTrue();
        assertThat(report.type()).isEqualTo(AnomalyType.LATENCY_SPIKE);
        assertThat(report.message()).contains("avg");
    }

    @Test
    void detectsTokenSpike() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AnomalyDetector detector = new AnomalyDetector(store);

        store.add(event(100, 10, 10));
        store.add(event(100, 10, 10));
        store.add(event(100, 10, 10));

        AnomalyReport report = detector.analyze(event(100, 100, 100));

        assertThat(report.hasAnomaly()).isTrue();
        assertThat(report.type()).isEqualTo(AnomalyType.TOKEN_SPIKE);
    }

    @Test
    void detectsBothSpikes() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AnomalyDetector detector = new AnomalyDetector(store);

        store.add(event(100, 10, 10));
        store.add(event(100, 10, 10));
        store.add(event(100, 10, 10));

        AnomalyReport report = detector.analyze(event(500, 100, 100));

        assertThat(report.hasAnomaly()).isTrue();
        assertThat(report.type()).isEqualTo(AnomalyType.LATENCY_AND_TOKEN_SPIKE);
    }

    @Test
    void noAnomalyForNormalCall() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AnomalyDetector detector = new AnomalyDetector(store);

        store.add(event(100, 10, 10));
        store.add(event(110, 11, 10));
        store.add(event(90, 9, 10));

        AnomalyReport report = detector.analyze(event(105, 10, 10));

        assertThat(report.hasAnomaly()).isFalse();
    }
}