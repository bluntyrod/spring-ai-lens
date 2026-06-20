package io.ailens.springailens.anomaly;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.AnomalyType;
import io.ailens.springailens.store.RingBufferEventStore;

import java.util.List;

public class AnomalyDetector {

    private static final double LATENCY_THRESHOLD = 2.0;
    private static final double TOKEN_THRESHOLD = 2.0;
    private static final int MIN_CALLS_FOR_BASELINE = 3;

    private final RingBufferEventStore store;

    public AnomalyDetector(RingBufferEventStore store) {
        this.store = store;
    }

    public AnomalyReport analyze(AiCallEvent event) {
        List<AiCallEvent> history = store.getAll();

        if (history.size() < MIN_CALLS_FOR_BASELINE) {
            return AnomalyReport.none();
        }

        double avgLatency = history.stream()
                .mapToLong(AiCallEvent::latencyMs)
                .average()
                .orElse(0);

        double avgTokens = history.stream()
                .mapToInt(e -> e.promptTokens() + e.completionTokens())
                .average()
                .orElse(0);

        boolean latencySpike = avgLatency > 0 &&
                event.latencyMs() > avgLatency * LATENCY_THRESHOLD;

        boolean tokenSpike = avgTokens > 0 &&
                (event.promptTokens() + event.completionTokens()) > avgTokens * TOKEN_THRESHOLD;

        if (latencySpike && tokenSpike) {
            return AnomalyReport.of(AnomalyType.LATENCY_AND_TOKEN_SPIKE,
                    "Latency %.0fms (avg %.0fms) and tokens %d (avg %.0f) both spiked"
                            .formatted(
                                    (double) event.latencyMs(), avgLatency,
                                    event.promptTokens() + event.completionTokens(), avgTokens
                            ));
        } else if (latencySpike) {
            return AnomalyReport.of(AnomalyType.LATENCY_SPIKE,
                    "Latency %.0fms is %.1fx above avg %.0fms"
                            .formatted((double) event.latencyMs(),
                                    event.latencyMs() / avgLatency, avgLatency));
        } else if (tokenSpike) {
            return AnomalyReport.of(AnomalyType.TOKEN_SPIKE,
                    "Token count %d is %.1fx above avg %.0f"
                            .formatted(event.promptTokens() + event.completionTokens(),
                                    (event.promptTokens() + event.completionTokens()) / avgTokens, avgTokens));
        }

        return AnomalyReport.none();
    }
}