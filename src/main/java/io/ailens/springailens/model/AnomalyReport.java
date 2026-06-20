package io.ailens.springailens.model;

public record AnomalyReport(
        boolean hasAnomaly,
        AnomalyType type,
        String message
) {
    public static AnomalyReport none() {
        return new AnomalyReport(false, null, null);
    }

    public static AnomalyReport of(AnomalyType type, String message) {
        return new AnomalyReport(true, type, message);
    }
}