package io.ailens.springailens.actuator;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.store.RingBufferEventStore;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;

@Endpoint(id = "ai-lens")
public class AiLensEndpoint {

    private final RingBufferEventStore store;

    public AiLensEndpoint(RingBufferEventStore store) {
        this.store = store;
    }

    @ReadOperation
    public AiLensReport report() {
        List<AiCallEvent> events = store.getAll();

        long avgLatency = (long) events.stream()
                .mapToLong(AiCallEvent::latencyMs)
                .average()
                .orElse(0);

        int totalTokens = events.stream()
                .mapToInt(e -> e.promptTokens() + e.completionTokens())
                .sum();

        return new AiLensReport(events, avgLatency, totalTokens, events.size());
    }

    public record AiLensReport(
            List<AiCallEvent> calls,
            long avgLatencyMs,
            int totalTokens,
            int totalCalls
    ) {}
}