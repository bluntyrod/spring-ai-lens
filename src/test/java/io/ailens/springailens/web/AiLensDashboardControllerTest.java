package io.ailens.springailens.web;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.store.RingBufferEventStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiLensDashboardControllerTest {

    @Test
    void dashboardShowsEmptyStateWhenNoEvents() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        AiLensDashboardController controller = new AiLensDashboardController(store);

        String html = controller.dashboard();

        assertThat(html).contains("No LLM calls captured yet");
        assertThat(html).contains("Total calls");
    }

    @Test
    void dashboardRendersEventData() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", "What is Java?", "A programming language", 42, 5, 10, AnomalyReport.none()));

        AiLensDashboardController controller = new AiLensDashboardController(store);
        String html = controller.dashboard();

        assertThat(html).contains("OpenAiChatModel");
        assertThat(html).contains("What is Java?");
        assertThat(html).contains("A programming language");
        assertThat(html).contains("42 ms");
    }

    @Test
    void dashboardTruncatesLongPrompts() {
        RingBufferEventStore store = new RingBufferEventStore(10);
        String longPrompt = "a".repeat(200);
        store.add(new AiCallEvent(UUID.randomUUID().toString(), Instant.now(),
                "OpenAiChatModel", longPrompt, "response", 10, 5, 5, AnomalyReport.none()));
        AiLensDashboardController controller = new AiLensDashboardController(store);
        String html = controller.dashboard();

        assertThat(html).contains("…");
        assertThat(html).doesNotContain(longPrompt);
    }
}