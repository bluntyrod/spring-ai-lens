package io.ailens.springailens.config;


public abstract class BaseStorageProperties {

    private int maxEvents = 500;

    public int getMaxEvents() { return maxEvents; }
    public void setMaxEvents(int maxEvents) { this.maxEvents = maxEvents; }
}
