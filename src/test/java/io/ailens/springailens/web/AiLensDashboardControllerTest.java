package io.ailens.springailens.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.util.EventStore;
import io.ailens.springailens.util.store.InMemoryEventStore;

class AiLensDashboardControllerTest {

    @Test
    void dashboardShowsEmptyStateWhenNoEvents() {
        EventStore store = new InMemoryEventStore(10);
        AiLensDashboardController controller = new AiLensDashboardController(store);

        String html = controller.dashboard();

        assertThat(html).contains("No LLM calls captured yet");
        assertThat(html).contains("Total calls");
    }

    @Test
    void dashboardRendersEventData() {
        EventStore store = new InMemoryEventStore(10);
        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", "What is Java?", "A programming language", 42, 5, 10, AnomalyReport.none(), null));

        AiLensDashboardController controller = new AiLensDashboardController(store);
        String html = controller.dashboard();

        assertThat(html).contains("OpenAiChatModel");
        assertThat(html).contains("What is Java?");
        assertThat(html).contains("A programming language");
        assertThat(html).contains("42 ms");
    }

    @Test
    void dashboardTruncatesLongPrompts() {
        EventStore store = new InMemoryEventStore(10);
        String longPrompt = "a".repeat(200);
        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", longPrompt, "response", 10, 5, 5, AnomalyReport.none(), null));
        AiLensDashboardController controller = new AiLensDashboardController(store);
        String html = controller.dashboard();

        assertThat(html).contains("…");
        assertThat(html).doesNotContain(longPrompt);
    }
}
