package io.ailens.springailens.util.interceptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.PromptDiffResult;
import io.ailens.springailens.util.anomaly.AnomalyDetector;
import io.ailens.springailens.util.diff.PromptDiffTracker;
import io.ailens.springailens.util.otel.AiLensOtelExporter;
import io.ailens.springailens.util.store.RingBufferEventStore;

@Aspect
public class AiLensInterceptor {

    private final RingBufferEventStore store;
    private final AnomalyDetector anomalyDetector;
    private final PromptDiffTracker diffTracker;
    private final Optional<AiLensOtelExporter> otelExporter;

    public AiLensInterceptor(RingBufferEventStore store, AnomalyDetector anomalyDetector,
                             PromptDiffTracker diffTracker) {
        this(store, anomalyDetector, diffTracker, Optional.empty());
    }

    public AiLensInterceptor(RingBufferEventStore store, AnomalyDetector anomalyDetector,
                             PromptDiffTracker diffTracker, Optional<AiLensOtelExporter> otelExporter) {
        this.store = store;
        this.anomalyDetector = anomalyDetector;
        this.diffTracker = diffTracker;
        this.otelExporter = otelExporter;
    }

    @Around("execution(* org.springframework.ai.chat.model.ChatModel.call(..))")
    public Object interceptCall(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();

        String promptText = "";
        Object[] args = pjp.getArgs();
        if (args.length > 0 && args[0] instanceof Prompt prompt) {
            promptText = prompt.getContents();
        }

        Object result = pjp.proceed();
        long latencyMs = System.currentTimeMillis() - start;

        String responseText = "";
        int promptTokens = 0;
        int completionTokens = 0;

        if (result instanceof ChatResponse response) {
            if (response.getResult() != null) {
                responseText = response.getResult().getOutput().getText();
            }
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                promptTokens = response.getMetadata().getUsage().getPromptTokens().intValue();
                completionTokens = response.getMetadata().getUsage().getCompletionTokens().intValue();
            }
        }

        AiCallEvent event = new AiCallEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                pjp.getTarget().getClass().getSimpleName(),
                promptText,
                responseText,
                latencyMs,
                promptTokens,
                completionTokens,
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
        otelExporter.ifPresent(exporter -> exporter.export(finalEvent));

        return result;
    }
}
