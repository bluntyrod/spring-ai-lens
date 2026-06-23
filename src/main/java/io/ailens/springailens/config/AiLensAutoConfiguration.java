package io.ailens.springailens.config;

import static io.ailens.springailens.model.StorageType.*;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import io.ailens.springailens.actuator.AiLensEndpoint;
import io.ailens.springailens.util.EventStore;
import io.ailens.springailens.util.advisor.AiLensStreamAdvisor;
import io.ailens.springailens.util.anomaly.AnomalyDetector;
import io.ailens.springailens.util.diff.PromptDiffTracker;
import io.ailens.springailens.util.interceptor.AiLensInterceptor;
import io.ailens.springailens.util.metrics.AiLensMetrics;
import io.ailens.springailens.util.otel.AiLensOtelExporter;
import io.ailens.springailens.util.store.InMemoryEventStore;
import io.ailens.springailens.util.store.PostgresEventStore;
import io.ailens.springailens.util.store.RedisEventStore;
import io.ailens.springailens.web.AiLensDashboardController;
import io.micrometer.core.instrument.MeterRegistry;

@AutoConfiguration
@EnableConfigurationProperties(AiLensProperties.class)
public class AiLensAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventStore.class)
    public EventStore aiLensEventStore(AiLensProperties properties,
                                       Optional<StringRedisTemplate> redisTemplate,
                                       Optional<JdbcTemplate> jdbcTemplate) {
        return switch (properties.getStorage().getType()) {
            case REDIS -> {
                if (redisTemplate.isEmpty()) {
                    throw new IllegalStateException(
                            "ai-lens.storage.type=redis but spring-boot-starter-data-redis is not on the classpath");
                }
                yield new RedisEventStore(redisTemplate.get(), properties);
            }
            case POSTGRES -> {
                if (jdbcTemplate.isEmpty()) {
                    throw new IllegalStateException(
                            "ai-lens.storage.type=postgres but spring-boot-starter-jdbc is not on the classpath");
                }
                yield new PostgresEventStore(jdbcTemplate.get(), properties);
            }
            case MEMORY -> new InMemoryEventStore(properties.getBufferSize());
        };
    }

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
    public AiLensInterceptor aiLensInterceptor(EventStore store,
                                               AnomalyDetector detector,
                                               PromptDiffTracker diffTracker,
                                               Optional<AiLensOtelExporter> otelExporter,
                                               Optional<AiLensMetrics> metrics) {
        return new AiLensInterceptor(store, detector, diffTracker, otelExporter, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensStreamAdvisor aiLensStreamAdvisor(EventStore store,
                                                   AnomalyDetector detector,
                                                   PromptDiffTracker diffTracker,
                                                   Optional<AiLensOtelExporter> otelExporter,
                                                   Optional<AiLensMetrics> metrics) {
        return new AiLensStreamAdvisor(store, detector, diffTracker, otelExporter, metrics);
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
