package com.tramchester.unit.cloud;


import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudWatchReporterTest extends EasyMockSupport {

    private ConfigFromInstanceUserData configFromInstanceUserData;

    @Before
    public void beforeEachTestRuns() {
        configFromInstanceUserData = createMock(ConfigFromInstanceUserData.class);
    }

    @Test
    public void shouldFormNamespaceCorrectly() {
        EasyMock.expect(configFromInstanceUserData.get("ENV")).andReturn("environment");

        replayAll();
        String result = CloudWatchReporter.formNamespace("com.tramchester",configFromInstanceUserData);
        verifyAll();
        assertThat(result).isEqualTo("environment:com:tramchester");
    }
}
