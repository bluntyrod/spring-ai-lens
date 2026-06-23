package io.ailens.springailens.util.store;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.ailens.springailens.config.AiLensProperties;
import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.util.EventStore;

public class RedisEventStore implements EventStore {

    private static final String EVENTS_KEY = "%s:events";

    private final StringRedisTemplate redis;
    private final AiLensProperties properties;
    private final ObjectMapper mapper;
    private final String eventsKey;

    public RedisEventStore(StringRedisTemplate redis, AiLensProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.eventsKey = EVENTS_KEY.formatted(
                properties.getStorage().getRedis().getKeyPrefix());
    }

    @Override
    public void add(AiCallEvent event) {
        try {
            String json = mapper.writeValueAsString(event);
            redis.opsForList().leftPush(eventsKey, json);
            redis.opsForList().trim(eventsKey, 0, properties.getBufferSize() - 1);
            int ttlDays = properties.getStorage().getRedis().getTtlDays();
            redis.expire(eventsKey, Duration.ofDays(ttlDays));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize AiCallEvent", e);
        }
    }

    @Override
    public List<AiCallEvent> getAll() {
        List<String> jsons = redis.opsForList()
                .range(eventsKey, 0, -1);
        if (jsons == null) return Collections.emptyList();
        Collections.reverse(jsons);
        return jsons.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<AiCallEvent> getRecent(int limit) {
        List<String> jsons = redis.opsForList()
                .range(eventsKey, 0, limit - 1);
        if (jsons == null) return Collections.emptyList();
        return jsons.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public long count() {
        Long size = redis.opsForList().size(eventsKey);
        return size != null ? size : 0;
    }

    @Override
    public void clear() {
        redis.delete(eventsKey);
    }

    private AiCallEvent deserialize(String json) {
        try {
            return mapper.readValue(json, AiCallEvent.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
