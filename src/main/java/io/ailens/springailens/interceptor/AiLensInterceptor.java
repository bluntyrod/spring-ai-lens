package io.ailens.springailens.interceptor;

import io.ailens.springailens.anomaly.AnomalyDetector;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.store.RingBufferEventStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.UUID;

@Aspect
public class AiLensInterceptor {

    private final RingBufferEventStore store;
    private final AnomalyDetector anomalyDetector;

    public AiLensInterceptor(RingBufferEventStore store, AnomalyDetector anomalyDetector) {
        this.store = store;
        this.anomalyDetector = anomalyDetector;
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
                AnomalyReport.none()
        );

        AnomalyReport anomaly = anomalyDetector.analyze(event);

        store.add(new AiCallEvent(
                event.id(), event.timestamp(), event.model(),
                event.prompt(), event.response(), event.latencyMs(),
                event.promptTokens(), event.completionTokens(),
                anomaly
        ));

        return result;
    }
}