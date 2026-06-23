package io.ailens.springailens.util.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import io.ailens.springailens.config.AnomalyProperties;
import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.util.EventStore;
import io.ailens.springailens.util.anomaly.AnomalyDetector;
import io.ailens.springailens.util.diff.PromptDiffTracker;
import io.ailens.springailens.util.store.InMemoryEventStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class AiLensStreamAdvisorTest {

    private EventStore store;
    private AiLensStreamAdvisor advisor;

    @BeforeEach
    void setUp() {
        store = new InMemoryEventStore(10);
        AnomalyDetector detector = new AnomalyDetector(store, new AnomalyProperties());
        PromptDiffTracker diffTracker = new PromptDiffTracker();
        advisor = new AiLensStreamAdvisor(store, detector, diffTracker, Optional.empty(), Optional.empty());
    }

    @Test
    void capturesStreamedCallEvent() {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("What is Java?")))
                .build();

        ChatClientResponse chunk1 = mockChunk("Java is ");
        ChatClientResponse chunk2 = mockChunk("a programming language.");

        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        when(chain.nextStream(request)).thenReturn(Flux.just(chunk1, chunk2));

        StepVerifier.create(advisor.adviseStream(request, chain))
                .expectNext(chunk1)
                .expectNext(chunk2)
                .verifyComplete();

        List<AiCallEvent> events = store.getAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).prompt()).isEqualTo("What is Java?");
        assertThat(events.get(0).response()).isEqualTo("Java is a programming language.");
        assertThat(events.get(0).latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void doesNotBlockStreamOnError() {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("What is Java?")))
                .build();

        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
        when(chain.nextStream(request)).thenReturn(Flux.error(new RuntimeException("LLM error")));

        StepVerifier.create(advisor.adviseStream(request, chain))
                .expectError(RuntimeException.class)
                .verify();

        assertThat(store.getAll()).isEmpty();
    }

    @Test
    void advisorNameAndOrderAreCorrect() {
        assertThat(advisor.getName()).isEqualTo("AiLensStreamAdvisor");
        assertThat(advisor.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    private ChatClientResponse mockChunk(String text) {
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(text))
        ));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .build();
    }
}
