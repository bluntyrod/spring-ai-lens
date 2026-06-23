package io.ailens.springailens.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

import io.ailens.springailens.config.AiLensProperties;
import io.ailens.springailens.config.RedisStorageProperties;
import io.ailens.springailens.config.StorageProperties;
import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.StorageType;
import io.ailens.springailens.util.store.RedisEventStore;

@Disabled
@Testcontainers
class RedisEventStoreTest {

    @Container
    static RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7-alpine"));

    private RedisEventStore store;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getFirstMappedPort());
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();

        AiLensProperties properties = buildProperties();
        store = new RedisEventStore(template, properties);
    }

    @AfterEach
    void tearDown() {
        store.clear();
        connectionFactory.destroy();
    }

    private AiCallEvent event(String prompt) {
        return new AiCallEvent(
                UUID.randomUUID().toString(), Instant.now(),
                "OllamaChatModel", prompt, "response",
                100, 10, 20, AnomalyReport.none(), null
        );
    }

    private AiLensProperties buildProperties() {
        AiLensProperties properties = new AiLensProperties();
        StorageProperties storage = new StorageProperties();
        storage.setType(StorageType.REDIS);
        RedisStorageProperties redisProps = new RedisStorageProperties();
        redisProps.setKeyPrefix("ai-lens-test");
        redisProps.setTtlDays(1);
        redisProps.setMaxEvents(3);
        storage.setRedis(redisProps);
        properties.setStorage(storage);
        return properties;
    }

    @Test
    void addsAndRetrievesEvents() {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));

        List<AiCallEvent> events = store.getAll();
        assertThat(events).hasSize(2);
        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    void evictsOldestWhenFull() {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));
        store.add(event("prompt 3"));
        store.add(event("prompt 4"));

        assertThat(store.count()).isEqualTo(3);
        assertThat(store.getAll())
                .extracting(AiCallEvent::prompt)
                .doesNotContain("prompt 1");
    }

    @Test
    void getRecentReturnsLastN() {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));
        store.add(event("prompt 3"));

        List<AiCallEvent> recent = store.getRecent(2);
        assertThat(recent).hasSize(2);
    }

    @Test
    void clearDeletesAllEvents() {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));
        store.clear();

        assertThat(store.getAll()).isEmpty();
        assertThat(store.count()).isEqualTo(0);
    }

    @Test
    void survivesClearAndReAdd() {
        store.add(event("before clear"));
        store.clear();
        store.add(event("after clear"));

        assertThat(store.getAll()).hasSize(1);
        assertThat(store.getAll().get(0).prompt()).isEqualTo("after clear");
    }
}
