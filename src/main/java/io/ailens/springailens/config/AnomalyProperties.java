package io.ailens.springailens.config;

public class AnomalyProperties {

    private double latencyThreshold = 2.0;
    private double tokenThreshold = 2.0;
    private int minBaselineCalls = 3;

    public double getLatencyThreshold() { return latencyThreshold; }
    public void setLatencyThreshold(double latencyThreshold) { this.latencyThreshold = latencyThreshold; }

    public double getTokenThreshold() { return tokenThreshold; }
    public void setTokenThreshold(double tokenThreshold) { this.tokenThreshold = tokenThreshold; }

    public int getMinBaselineCalls() { return minBaselineCalls; }
    public void setMinBaselineCalls(int minBaselineCalls) { this.minBaselineCalls = minBaselineCalls; }
}
