package com.tramchester.unit.cloud;


import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudWatchReporterTest extends EasyMockSupport {

    private ConfigFromInstanceUserData configFromInstanceUserData;

    @BeforeEach
    void beforeEachTestRuns() {
        configFromInstanceUserData = createMock(ConfigFromInstanceUserData.class);
    }

    @Test
    void shouldFormNamespaceCorrectly() {
        EasyMock.expect(configFromInstanceUserData.get("ENV")).andReturn("environment");

        replayAll();
        String result = CloudWatchReporter.formNamespace("com.tramchester",configFromInstanceUserData);
        verifyAll();
        assertThat(result).isEqualTo("environment:com:tramchester");
    }
}
