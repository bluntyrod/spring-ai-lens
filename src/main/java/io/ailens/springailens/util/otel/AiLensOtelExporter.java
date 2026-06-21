package io.ailens.springailens.util.otel;

import io.ailens.springailens.model.AiCallEvent;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class AiLensOtelExporter {

    private static final String INSTRUMENTATION_NAME = "spring-ai-lens";
    private final Tracer tracer;

    public AiLensOtelExporter() {
        this.tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    public void export(AiCallEvent event) {
        Span span = tracer.spanBuilder("llm.call")
                .startSpan();

        try {
            span.setAttribute("llm.model", event.model());
            span.setAttribute("llm.prompt", truncate(event.prompt(), 500));
            span.setAttribute("llm.response", truncate(event.response(), 500));
            span.setAttribute("llm.latency_ms", event.latencyMs());
            span.setAttribute("llm.prompt_tokens", event.promptTokens());
            span.setAttribute("llm.completion_tokens", event.completionTokens());
            span.setAttribute("llm.total_tokens", event.promptTokens() + event.completionTokens());

            if (event.anomaly() != null && event.anomaly().hasAnomaly()) {
                span.setAttribute("llm.anomaly", true);
                span.setAttribute("llm.anomaly_type", event.anomaly().type().name());
                span.setAttribute("llm.anomaly_message", event.anomaly().message());
                span.setStatus(StatusCode.ERROR, event.anomaly().message());
            }

            if (event.diff() != null && event.diff().hasChanged()) {
                span.setAttribute("llm.prompt_changed", true);
                span.setAttribute("llm.prompt_hash", event.diff().currentHash());
                span.setAttribute("llm.previous_prompt_hash", event.diff().previousHash());
            }

        } finally {
            span.end();
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
