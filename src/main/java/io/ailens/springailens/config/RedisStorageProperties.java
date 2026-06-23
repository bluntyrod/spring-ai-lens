package io.ailens.springailens.config;

public class RedisStorageProperties extends BaseStorageProperties {

    private String keyPrefix = "ai-lens";
    private int ttlDays = 7;

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public int getTtlDays() { return ttlDays; }
    public void setTtlDays(int ttlDays) { this.ttlDays = ttlDays; }
}
