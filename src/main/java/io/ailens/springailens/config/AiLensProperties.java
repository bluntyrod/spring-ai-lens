package io.ailens.springailens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-lens")
public class AiLensProperties {

    private int bufferSize = 500;
    private AnomalyProperties anomaly = new AnomalyProperties();
    private DashboardProperties dashboard = new DashboardProperties();
    private StorageProperties storage = new StorageProperties();

    public int getBufferSize() { return bufferSize; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }

    public AnomalyProperties getAnomaly() { return anomaly; }
    public void setAnomaly(AnomalyProperties anomaly) { this.anomaly = anomaly; }

    public DashboardProperties getDashboard() { return dashboard; }
    public void setDashboard(DashboardProperties dashboard) { this.dashboard = dashboard; }

    public StorageProperties getStorage() { return storage; }
    public void setStorage(StorageProperties storage) { this.storage = storage; }
}
