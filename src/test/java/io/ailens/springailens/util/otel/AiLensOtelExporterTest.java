package io.ailens.springailens.util.otel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.AnomalyType;
import io.ailens.springailens.model.DiffStatus;
import io.ailens.springailens.model.PromptDiffResult;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

class AiLensOtelExporterTest {

    private InMemorySpanExporter spanExporter;
    private AiLensOtelExporter otelExporter;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
        otelExporter = new AiLensOtelExporter();
    }

    @AfterEach
    void tearDown() {
        GlobalOpenTelemetry.resetForTest();
        spanExporter.reset();
    }

    @Test
    void exportsSpanWithLlmAttributes() {
        AiCallEvent event = new AiCallEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                "OllamaChatModel",
                "What is Java?",
                "A programming language",
                150,
                10,
                20,
                AnomalyReport.none(),
                null
        );

        otelExporter.export(event);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);

        SpanData span = spans.get(0);
        assertThat(span.getName()).isEqualTo("llm.call");
        assertThat(span.getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.stringKey("llm.model")))
                .isEqualTo("OllamaChatModel");
        assertThat(span.getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.longKey("llm.latency_ms")))
                .isEqualTo(150L);
        assertThat(span.getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.longKey("llm.total_tokens")))
                .isEqualTo(30L);
    }

    @Test
    void setsErrorStatusOnAnomaly() {
        AiCallEvent event = new AiCallEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                "OllamaChatModel",
                "What is Java?",
                "A programming language",
                500,
                10,
                20,
                AnomalyReport.of(AnomalyType.LATENCY_SPIKE, "Latency spike detected"),
                null
        );

        otelExporter.export(event);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.booleanKey("llm.anomaly")))
                .isTrue();
    }

    @Test
    void setsPromptChangedOnDiff() {
        AiCallEvent event = new AiCallEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                "OllamaChatModel",
                "What is Python?",
                "A language",
                100,
                10,
                10,
                AnomalyReport.none(),
                new PromptDiffResult(DiffStatus.CHANGED, "abc123", "def456", "old prompt", "new prompt")
        );

        otelExporter.export(event);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans.get(0).getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.booleanKey("llm.prompt_changed")))
                .isTrue();
    }
}
