package io.ailens.springailens.config;

public class PostgresStorageProperties extends BaseStorageProperties {

    private int batchSize = 50;
    private int flushIntervalMs = 100;

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public int getFlushIntervalMs() { return flushIntervalMs; }
    public void setFlushIntervalMs(int flushIntervalMs) { this.flushIntervalMs = flushIntervalMs; }
}
