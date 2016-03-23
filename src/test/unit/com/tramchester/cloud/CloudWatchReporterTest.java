package com.tramchester.cloud;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudWatchReporterTest {

    @Test
    public void shouldFormNamespaceCorrectly() {
        String result = CloudWatchReporter.formNamespace("com.tramchester");
        assertThat(result).endsWith(":com:tramchester");
    }
}
