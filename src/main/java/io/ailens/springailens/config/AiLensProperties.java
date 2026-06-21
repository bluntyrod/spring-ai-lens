package io.ailens.springailens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-lens")
public class AiLensProperties {

    private int bufferSize = 500;
    private Anomaly anomaly = new Anomaly();
    private Dashboard dashboard = new Dashboard();

    public int getBufferSize() { return bufferSize; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }

    public Anomaly getAnomaly() { return anomaly; }
    public void setAnomaly(Anomaly anomaly) { this.anomaly = anomaly; }

    public Dashboard getDashboard() { return dashboard; }
    public void setDashboard(Dashboard dashboard) { this.dashboard = dashboard; }

    public static class Anomaly {
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

    public static class Dashboard {
        private boolean enabled = true;
        private String path = "/ai-lens";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}
