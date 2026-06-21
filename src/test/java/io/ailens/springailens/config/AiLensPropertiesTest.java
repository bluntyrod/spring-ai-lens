package io.ailens.springailens.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiLensPropertiesTest {

    @Test
    void defaultValuesAreCorrect() {
        AiLensProperties properties = new AiLensProperties();

        assertThat(properties.getBufferSize()).isEqualTo(500);
        assertThat(properties.getAnomaly().getLatencyThreshold()).isEqualTo(2.0);
        assertThat(properties.getAnomaly().getTokenThreshold()).isEqualTo(2.0);
        assertThat(properties.getAnomaly().getMinBaselineCalls()).isEqualTo(3);
        assertThat(properties.getDashboard().isEnabled()).isTrue();
        assertThat(properties.getDashboard().getPath()).isEqualTo("/ai-lens");
    }

    @Test
    void customValuesAreApplied() {
        AiLensProperties properties = new AiLensProperties();
        properties.setBufferSize(100);
        properties.getAnomaly().setLatencyThreshold(3.0);
        properties.getAnomaly().setTokenThreshold(4.0);
        properties.getAnomaly().setMinBaselineCalls(5);
        properties.getDashboard().setEnabled(false);
        properties.getDashboard().setPath("/custom-lens");

        assertThat(properties.getBufferSize()).isEqualTo(100);
        assertThat(properties.getAnomaly().getLatencyThreshold()).isEqualTo(3.0);
        assertThat(properties.getAnomaly().getTokenThreshold()).isEqualTo(4.0);
        assertThat(properties.getAnomaly().getMinBaselineCalls()).isEqualTo(5);
        assertThat(properties.getDashboard().isEnabled()).isFalse();
        assertThat(properties.getDashboard().getPath()).isEqualTo("/custom-lens");
    }
}
