package io.ailens.springailens.config;

public class DashboardProperties {

    private boolean enabled = true;
    private String path = "/ai-lens";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
