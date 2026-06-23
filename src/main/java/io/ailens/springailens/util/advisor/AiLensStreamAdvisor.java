package io.ailens.springailens.util.advisor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.PromptDiffResult;
import io.ailens.springailens.util.EventStore;
import io.ailens.springailens.util.anomaly.AnomalyDetector;
import io.ailens.springailens.util.diff.PromptDiffTracker;
import io.ailens.springailens.util.metrics.AiLensMetrics;
import io.ailens.springailens.util.otel.AiLensOtelExporter;
import reactor.core.publisher.Flux;

public class AiLensStreamAdvisor implements StreamAdvisor {

    private final EventStore store;
    private final AnomalyDetector anomalyDetector;
    private final PromptDiffTracker diffTracker;
    private final Optional<AiLensOtelExporter> otelExporter;
    private final Optional<AiLensMetrics> metrics;
    public AiLensStreamAdvisor(EventStore store,
                               AnomalyDetector anomalyDetector,
                               PromptDiffTracker diffTracker,
                               Optional<AiLensOtelExporter> otelExporter, Optional<AiLensMetrics> metrics) {
        this.store = store;
        this.anomalyDetector = anomalyDetector;
        this.diffTracker = diffTracker;
        this.otelExporter = otelExporter;
        this.metrics = metrics;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request,
                                                 StreamAdvisorChain chain) {
        long startTime = System.currentTimeMillis();
        String promptText = request.prompt().getContents();
        AtomicReference<StringBuilder> responseBuilder = new AtomicReference<>(new StringBuilder());
        AtomicLong promptTokens = new AtomicLong(0);
        AtomicLong completionTokens = new AtomicLong(0);
        AtomicReference<String> modelName = new AtomicReference<>("unknown");

        return chain.nextStream(request)
                .doOnNext(response -> {
                    if (response.chatResponse() != null) {
                        var result = response.chatResponse().getResult();
                        if (result != null && result.getOutput() != null
                                && result.getOutput().getText() != null) {
                            responseBuilder.get().append(result.getOutput().getText());
                        }
                        var metadata = response.chatResponse().getMetadata();
                        if (metadata != null) {
                            if (metadata.getModel() != null) {
                                modelName.set(metadata.getModel());
                            }
                            if (metadata.getUsage() != null) {
                                if (metadata.getUsage().getPromptTokens() != null) {
                                    promptTokens.set(metadata.getUsage().getPromptTokens());
                                }
                                if (metadata.getUsage().getCompletionTokens() != null) {
                                    completionTokens.set(metadata.getUsage().getCompletionTokens());
                                }
                            }
                        }
                    }
                })
                .doOnComplete(() -> {
                    long latencyMs = System.currentTimeMillis() - startTime;

                    AiCallEvent event = new AiCallEvent(
                            UUID.randomUUID().toString(),
                            Instant.now(),
                            modelName.get(),
                            promptText,
                            responseBuilder.get().toString(),
                            latencyMs,
                            (int) promptTokens.get(),
                            (int) completionTokens.get(),
                            AnomalyReport.none(),
                            null
                    );

                    AnomalyReport anomaly = anomalyDetector.analyze(event);
                    PromptDiffResult diff = diffTracker.track(event.model(), event.prompt());

                    AiCallEvent finalEvent = new AiCallEvent(
                            event.id(), event.timestamp(), event.model(),
                            event.prompt(), event.response(), event.latencyMs(),
                            event.promptTokens(), event.completionTokens(),
                            anomaly, diff
                    );

                    store.add(finalEvent);
                    otelExporter.ifPresent(e -> e.export(finalEvent));
                    metrics.ifPresent(m->m.record(finalEvent));
                });
    }

    @Override
    public String getName() {
        return "AiLensStreamAdvisor";
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE; // run first so we capture everything
    }
}
