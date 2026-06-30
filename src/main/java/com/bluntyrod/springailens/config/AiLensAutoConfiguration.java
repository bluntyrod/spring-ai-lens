package com.bluntyrod.springailens.config;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.bluntyrod.springailens.actuator.AiLensEndpoint;
import com.bluntyrod.springailens.util.EventStore;
import com.bluntyrod.springailens.util.advisor.AiLensStreamAdvisor;
import com.bluntyrod.springailens.util.anomaly.AnomalyDetector;
import com.bluntyrod.springailens.util.diff.PromptDiffTracker;
import com.bluntyrod.springailens.util.diff.PromptRegressionAlerter;
import com.bluntyrod.springailens.util.interceptor.AiLensInterceptor;
import com.bluntyrod.springailens.util.metrics.AiLensMetrics;
import com.bluntyrod.springailens.util.otel.AiLensOtelExporter;
import com.bluntyrod.springailens.web.AiLensDashboardController;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;

@AutoConfiguration
@EnableConfigurationProperties(AiLensProperties.class)
public class AiLensAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AnomalyDetector aiLensAnomalyDetector(EventStore store,
                                                 AiLensProperties properties) {
        return new AnomalyDetector(store, properties.getAnomaly());
    }

    @Bean
    @ConditionalOnMissingBean
    public PromptDiffTracker aiLensPromptDiffTracker() {
        return new PromptDiffTracker();
    }

    @Bean
    @ConditionalOnMissingBean
    public PromptRegressionAlerter aiLensPromptRegressionAlerter(AiLensProperties properties) {
        return new PromptRegressionAlerter(properties.getDiff());
    }

    /**
     * Primary path: Spring Boot (via micrometer-tracing's OTel bridge, or any
     * starter that registers an OpenTelemetry bean) already manages an
     * {@link OpenTelemetry} instance configured for whatever backend the user
     * set up via {@code management.tracing.*}/{@code management.otlp.*}
     * properties — Tempo, Honeycomb, Datadog Agent, Zipkin, Jaeger-via-OTLP,
     * anything. We just reuse it.
     */
    @Bean
    @ConditionalOnMissingBean(AiLensOtelExporter.class)
    @ConditionalOnBean(OpenTelemetry.class)
    public AiLensOtelExporter aiLensOtelExporter(OpenTelemetry openTelemetry) {
        return new AiLensOtelExporter(openTelemetry);
    }

    /**
     * Fallback path: no Spring-managed OpenTelemetry bean exists (e.g. tracing
     * is wired purely via a Java agent, which registers only the static
     * GlobalOpenTelemetry singleton). Still works, just one step removed from
     * Spring's own autoconfiguration.
     */
    @Bean
    @ConditionalOnMissingBean({AiLensOtelExporter.class, OpenTelemetry.class})
    @ConditionalOnClass(name = "io.opentelemetry.api.GlobalOpenTelemetry")
    public AiLensOtelExporter aiLensOtelExporterGlobalFallback() {
        return new AiLensOtelExporter(io.opentelemetry.api.GlobalOpenTelemetry.get());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    public AiLensMetrics aiLensMetrics(MeterRegistry registry) {
        return new AiLensMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensInterceptor aiLensInterceptor(EventStore store,
                                               AnomalyDetector detector,
                                               PromptDiffTracker diffTracker,
                                               Optional<AiLensOtelExporter> otelExporter,
                                               Optional<AiLensMetrics> metrics,
                                               Optional<PromptRegressionAlerter> regressionAlerter) {
        return new AiLensInterceptor(store, detector, diffTracker, otelExporter, metrics, regressionAlerter);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensStreamAdvisor aiLensStreamAdvisor(EventStore store,
                                                   AnomalyDetector detector,
                                                   PromptDiffTracker diffTracker,
                                                   Optional<AiLensOtelExporter> otelExporter,
                                                   Optional<AiLensMetrics> metrics,
                                                   Optional<PromptRegressionAlerter> regressionAlerter) {
        return new AiLensStreamAdvisor(store, detector, diffTracker, otelExporter, metrics, regressionAlerter);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensEndpoint aiLensEndpoint(EventStore store) {
        return new AiLensEndpoint(store);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ai-lens.dashboard", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AiLensDashboardController aiLensDashboardController(EventStore store) {
        return new AiLensDashboardController(store);
    }
}