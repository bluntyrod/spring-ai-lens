package com.bluntyrod.springailens.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.bluntyrod.springailens.util.EventStore;
import com.bluntyrod.springailens.util.store.RedisEventStore;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@ConditionalOnProperty(prefix = "ai-lens.storage", name = "type", havingValue = "redis")
public class AiLensRedisAutoConfiguration {

    @Bean
    @Primary
    public EventStore aiLensEventStore(ApplicationContext ctx, AiLensProperties properties) {
        Object redisTemplate = ctx.getBean("stringRedisTemplate");
        return new RedisEventStore((org.springframework.data.redis.core.StringRedisTemplate) redisTemplate, properties);
    }
}
