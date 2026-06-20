package io.ailens.springailens.config;

import io.ailens.springailens.actuator.AiLensEndpoint;
import io.ailens.springailens.anomaly.AnomalyDetector;
import io.ailens.springailens.interceptor.AiLensInterceptor;
import io.ailens.springailens.store.RingBufferEventStore;
import io.ailens.springailens.web.AiLensDashboardController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AiLensAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RingBufferEventStore aiLensEventStore() {
        return new RingBufferEventStore(500);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnomalyDetector aiLensAnomalyDetector(RingBufferEventStore store) {
        return new AnomalyDetector(store);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensInterceptor aiLensInterceptor(RingBufferEventStore store, AnomalyDetector detector) {
        return new AiLensInterceptor(store, detector);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensEndpoint aiLensEndpoint(RingBufferEventStore store) {
        return new AiLensEndpoint(store);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiLensDashboardController aiLensDashboardController(RingBufferEventStore store) {
        return new AiLensDashboardController(store);
    }
}