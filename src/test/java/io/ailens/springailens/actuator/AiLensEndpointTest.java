package io.ailens.springailens.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.util.EventStore;
import io.ailens.springailens.util.store.InMemoryEventStore;

class AiLensEndpointTest {

    @Test
    void reportIsEmptyWhenNoCallsMade() {
        EventStore store = new InMemoryEventStore(10);
        AiLensEndpoint endpoint = new AiLensEndpoint(store);

        AiLensEndpoint.AiLensReport report = endpoint.report();

        assertThat(report.totalCalls()).isEqualTo(0);
        assertThat(report.avgLatencyMs()).isEqualTo(0);
        assertThat(report.totalTokens()).isEqualTo(0);
        assertThat(report.calls()).isEmpty();
    }

    @Test
    void reportAggregatesCorrectly() {
        EventStore store = new InMemoryEventStore(10);
        AiLensEndpoint endpoint = new AiLensEndpoint(store);

        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", "prompt 1", "response 1", 100, 10, 20, AnomalyReport.none(), null));
        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", "prompt 2", "response 2", 200, 15, 25, AnomalyReport.none(), null));        AiLensEndpoint.AiLensReport report = endpoint.report();

        assertThat(report.totalCalls()).isEqualTo(2);
        assertThat(report.avgLatencyMs()).isEqualTo(150);
        assertThat(report.totalTokens()).isEqualTo(70); // 10+20+15+25
    }
}
