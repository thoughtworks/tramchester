package com.tramchester.unit.deployment;

import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.deployment.UploadRemoteSourceData;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UploadRemoteSourceDataTest extends EasyMockSupport {

    private UploadFileToS3 s3Uploader;
    private UploadRemoteSourceData uploadRemoteData;

    @BeforeEach
    void beforeEachTestRuns() {

        s3Uploader = createStrictMock(UploadFileToS3.class);

        List<RemoteDataSourceConfig> remoteConfigs = new ArrayList<>();

        remoteConfigs.add(new DataSourceConfig(Path.of("data/tram"), "fileA.zip"));
        remoteConfigs.add(new DataSourceConfig(Path.of("data/bus"), "fileB.zip"));
        remoteConfigs.add(new DataSourceConfig(Path.of("data/naptan"), "fileC.xml"));

        TramchesterConfig config = new ConfigWithRemoteSource(remoteConfigs);
        uploadRemoteData = new UploadRemoteSourceData(s3Uploader, config);
    }

    @Test
    void shouldUpdateEachRemoteDataSourceInConfigToS3() {

        final String prefix = "aPrefix";
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/tram/fileA.zip"), true)).andReturn(true);
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/bus/fileB.zip"), true)).andReturn(true);

        EasyMock.expect(s3Uploader.uploadFileZipped(prefix, Path.of("data/naptan/fileC.xml"), true)).andReturn(true);

        replayAll();
        boolean result = uploadRemoteData.upload(prefix);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldFailIfAnyFail() {

        final String prefix = "somePrefix";
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/tram/fileA.zip"), true)).andReturn(true);
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/bus/fileB.zip"), true)).andReturn(false);

        replayAll();
        boolean result = uploadRemoteData.upload(prefix);
        verifyAll();

        assertFalse(result);
    }

    private static class ConfigWithRemoteSource extends TestConfig {
        private final List<RemoteDataSourceConfig> remoteConfigs;

        private ConfigWithRemoteSource(List<RemoteDataSourceConfig> remoteConfigs) {
            this.remoteConfigs = remoteConfigs;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return remoteConfigs;
        }

        @Override
        public String getDistributionBucket() {
            return "bucket";
        }
    }

    private static class DataSourceConfig implements RemoteDataSourceConfig {
        private final Path path;
        private final String filename;

        private DataSourceConfig(Path path, String filename) {
            this.path = path;
            this.filename = filename;
        }

        @Override
        public Path getDataPath() {
            return path;
        }

        @Override
        public String getDataCheckUrl() {
            return "";
        }

        @Override
        public String getDataUrl() {
            return "";
        }

        @Override
        public String getDownloadFilename() {
            return filename;
        }

        @Override
        public String getName() {
            return filename;
        }

        @Override
        public boolean getIsS3() {
            return false;
        }
    }
}
