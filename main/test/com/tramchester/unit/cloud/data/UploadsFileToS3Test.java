package com.tramchester.unit.cloud.data;

import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UploadsFileToS3Test extends EasyMockSupport {

    private ClientForS3 clientForS3;
    private UploadFileToS3 uploadFileToS3;

    @BeforeEach
    void beforeEachTestRuns() {

        clientForS3 = createStrictMock(ClientForS3.class);

        uploadFileToS3 = new UploadFileToS3(clientForS3, new Configuration());
    }

    @Test
    void shouldUploadIfNotExists() {

        Path path = Path.of("data","tram", "file.zip");

        EasyMock.expect(clientForS3.isStarted()).andReturn(true);
        EasyMock.expect(clientForS3.keyExists("bucket", "prefix", "file.zip")).andReturn(false);
        EasyMock.expect(clientForS3.upload("bucket", "prefix/file.zip", path)).andReturn(true);

        replayAll();
        boolean result = uploadFileToS3.uploadFile("prefix", path);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldNotUploadIfExists() {

        Path path = Path.of("file.zip");

        EasyMock.expect(clientForS3.isStarted()).andReturn(true);
        EasyMock.expect(clientForS3.keyExists("bucket", "prefix", "file.zip")).andReturn(true);

        replayAll();
        boolean result = uploadFileToS3.uploadFile("prefix", path);
        verifyAll();

        assertFalse(result);
    }

    private static class Configuration extends TestConfig {

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public String getDistributionBucket() {
            return "bucket";
        }
    }

}
