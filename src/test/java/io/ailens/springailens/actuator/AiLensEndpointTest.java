package io.ailens.springailens.actuator;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.store.RingBufferEventStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiLensEndpointTest {

    @Test
    void reportIsEmptyWhenNoCallsMade() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AiLensEndpoint endpoint = new AiLensEndpoint(store);

        AiLensEndpoint.AiLensReport report = endpoint.report();

        assertThat(report.totalCalls()).isEqualTo(0);
        assertThat(report.avgLatencyMs()).isEqualTo(0);
        assertThat(report.totalTokens()).isEqualTo(0);
        assertThat(report.calls()).isEmpty();
    }

    @Test
    void reportAggregatesCorrectly() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AiLensEndpoint endpoint = new AiLensEndpoint(store);

        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", "prompt 1", "response 1", 100, 10, 20));
        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", "prompt 2", "response 2", 200, 15, 25));

        AiLensEndpoint.AiLensReport report = endpoint.report();

        assertThat(report.totalCalls()).isEqualTo(2);
        assertThat(report.avgLatencyMs()).isEqualTo(150);
        assertThat(report.totalTokens()).isEqualTo(70); // 10+20+15+25
    }
}