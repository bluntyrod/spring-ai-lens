package io.ailens.springailens.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.util.store.InMemoryEventStore;

class InMemoryEventStoreTest {

    private InMemoryEventStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryEventStore(3);
    }

    private AiCallEvent event(String prompt) {
        return new AiCallEvent(
                UUID.randomUUID().toString(), Instant.now(),
                "OllamaChatModel", prompt, "response",
                100, 10, 20, AnomalyReport.none(), null
        );
    }

    @Test
    void addsAndRetrievesEvents() {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));

        assertThat(store.getAll()).hasSize(2);
        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    void evictsOldestWhenFull() {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));
        store.add(event("prompt 3"));
        store.add(event("prompt 4"));

        List<AiCallEvent> events = store.getAll();
        assertThat(events).hasSize(3);
        assertThat(events.get(0).prompt()).isEqualTo("prompt 2");
        assertThat(events.get(2).prompt()).isEqualTo("prompt 4");
    }

    @Test
    void getRecentReturnsLastN() {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));
        store.add(event("prompt 3"));

        List<AiCallEvent> recent = store.getRecent(2);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(1).prompt()).isEqualTo("prompt 3");
    }

    @Test
    void clearEmptiesStore() {
        store.add(event("prompt 1"));
        store.clear();

        assertThat(store.getAll()).isEmpty();
        assertThat(store.count()).isEqualTo(0);
    }
}
