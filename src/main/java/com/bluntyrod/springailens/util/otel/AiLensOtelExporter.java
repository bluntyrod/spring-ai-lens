package com.bluntyrod.springailens.util.otel;

import com.bluntyrod.springailens.model.AiCallEvent;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class AiLensOtelExporter {

    private static final String INSTRUMENTATION_NAME = "spring-ai-lens";
    private final Tracer tracer;

    /**
     * Takes an {@link OpenTelemetry} instance rather than reaching for
     * {@code GlobalOpenTelemetry}. This is the vendor-neutral entry point of the
     * OTel SDK: whichever exporter that instance is configured with (OTLP to
     * Grafana Tempo, Honeycomb, Datadog Agent, a Zipkin exporter, Jaeger via OTLP,
     * etc.) just works — this class never hardcodes a backend.
     */
    public AiLensOtelExporter(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_NAME);
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