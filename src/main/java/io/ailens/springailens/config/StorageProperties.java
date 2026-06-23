package io.ailens.springailens.config;

import static io.ailens.springailens.model.StorageType.*;

import io.ailens.springailens.model.StorageType;

public class StorageProperties {

    private StorageType type = StorageType.MEMORY;
    private InMemoryStorageProperties memory = new InMemoryStorageProperties();
    private RedisStorageProperties redis = new RedisStorageProperties();
    private PostgresStorageProperties postgres = new PostgresStorageProperties();

    public StorageType getType() { return type; }
    public void setType(StorageType type) { this.type = type; }

    public InMemoryStorageProperties getMemory() { return memory; }
    public void setMemory(InMemoryStorageProperties memory) { this.memory = memory; }

    public RedisStorageProperties getRedis() { return redis; }
    public void setRedis(RedisStorageProperties redis) { this.redis = redis; }

    public PostgresStorageProperties getPostgres() { return postgres; }
    public void setPostgres(PostgresStorageProperties postgres) { this.postgres = postgres; }

    public BaseStorageProperties active() {
        return switch (type) {
            case REDIS -> redis;
            case POSTGRES -> postgres;
            case MEMORY -> memory;
        };
    }
}
