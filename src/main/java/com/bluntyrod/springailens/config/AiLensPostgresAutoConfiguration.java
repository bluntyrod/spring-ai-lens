package com.bluntyrod.springailens.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.bluntyrod.springailens.util.EventStore;
import com.bluntyrod.springailens.util.store.PostgresEventStore;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.jdbc.core.JdbcTemplate")
@ConditionalOnProperty(prefix = "ai-lens.storage", name = "type", havingValue = "postgres")
public class AiLensPostgresAutoConfiguration {

    @Bean
    @Primary
    public EventStore aiLensEventStore(ApplicationContext ctx, AiLensProperties properties) {
        Object jdbcTemplate = ctx.getBean("jdbcTemplate");
        return new PostgresEventStore((org.springframework.jdbc.core.JdbcTemplate) jdbcTemplate, properties);
    }
}
