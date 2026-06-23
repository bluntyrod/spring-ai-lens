package io.ailens.springailens.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.ailens.springailens.model.StorageType;

class StoragePropertiesTest {

    @Test
    void defaultsToMemoryType() {
        StorageProperties props = new StorageProperties();
        assertThat(props.getType()).isEqualTo(StorageType.MEMORY);
    }

    @Test
    void activeReturnsMemoryPropertiesByDefault() {
        StorageProperties props = new StorageProperties();
        assertThat(props.active()).isInstanceOf(InMemoryStorageProperties.class);
    }

    @Test
    void activeReturnsRedisPropertiesWhenRedisType() {
        StorageProperties props = new StorageProperties();
        props.setType(StorageType.REDIS);
        assertThat(props.active()).isInstanceOf(RedisStorageProperties.class);
    }

    @Test
    void activeReturnsPostgresPropertiesWhenPostgresType() {
        StorageProperties props = new StorageProperties();
        props.setType(StorageType.POSTGRES);
        assertThat(props.active()).isInstanceOf(PostgresStorageProperties.class);
    }

    @Test
    void redisDefaultValues() {
        RedisStorageProperties redis = new RedisStorageProperties();
        assertThat(redis.getKeyPrefix()).isEqualTo("ai-lens");
        assertThat(redis.getTtlDays()).isEqualTo(7);
        assertThat(redis.getMaxEvents()).isEqualTo(500);
    }

    @Test
    void postgresDefaultValues() {
        PostgresStorageProperties postgres = new PostgresStorageProperties();
        assertThat(postgres.getBatchSize()).isEqualTo(50);
        assertThat(postgres.getFlushIntervalMs()).isEqualTo(100);
        assertThat(postgres.getMaxEvents()).isEqualTo(500);
    }
}
