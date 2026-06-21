package io.ailens.springailens.config;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.ailens.springailens.actuator.AiLensEndpoint;
import io.ailens.springailens.util.advisor.AiLensStreamAdvisor;
import io.ailens.springailens.util.anomaly.AnomalyDetector;
import io.ailens.springailens.util.diff.PromptDiffTracker;
import io.ailens.springailens.util.interceptor.AiLensInterceptor;
import io.ailens.springailens.util.metrics.AiLensMetrics;
import io.ailens.springailens.util.otel.AiLensOtelExporter;
import io.ailens.springailens.util.store.RingBufferEventStore;
import io.ailens.springailens.web.AiLensDashboardController;
import io.micrometer.core.instrument.MeterRegistry;

@AutoConfiguration
@EnableConfigurationProperties(AiLensProperties.class)
public class AiLensAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RingBufferEventStore aiLensEventStore(AiLensProperties properties) {
        return new RingBufferEventStore(properties.getBufferSize());
    }

    @Bean
    @ConditionalOnMissingBean
    public AnomalyDetector aiLensAnomalyDetector(RingBufferEventStore store,
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
    @ConditionalOnClass(name = "io.opentelemetry.api.GlobalOpenTelemetry")
    public AiLensOtelExporter aiLensOtelExporter() {
        return new AiLensOtelExporter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    public AiLensMetrics aiLensMetrics(MeterRegistry registry) {
        return new AiLensMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensInterceptor aiLensInterceptor(RingBufferEventStore store,
                                               AnomalyDetector detector,
                                               PromptDiffTracker diffTracker,
                                               Optional<AiLensOtelExporter> otelExporter,
                                               Optional<AiLensMetrics> metrics) {
        return new AiLensInterceptor(store, detector, diffTracker, otelExporter, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensStreamAdvisor aiLensStreamAdvisor(RingBufferEventStore store,
                                                   AnomalyDetector detector,
                                                   PromptDiffTracker diffTracker,
                                                   Optional<AiLensOtelExporter> otelExporter,
                                                   Optional<AiLensMetrics> metrics) {
        return new AiLensStreamAdvisor(store, detector, diffTracker, otelExporter, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensEndpoint aiLensEndpoint(RingBufferEventStore store) {
        return new AiLensEndpoint(store);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ai-lens.dashboard", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AiLensDashboardController aiLensDashboardController(RingBufferEventStore store) {
        return new AiLensDashboardController(store);
    }

}
