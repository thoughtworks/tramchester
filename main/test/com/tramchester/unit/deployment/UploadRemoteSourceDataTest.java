package com.tramchester.unit.deployment;

import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.deployment.UploadRemoteSourceData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UploadRemoteSourceDataTest extends EasyMockSupport {

    private UploadFileToS3 s3Uploader;
    private UploadRemoteSourceData uploadRemoteData;
    private RemoteDataAvailable dataRefreshed;

    @BeforeEach
    void beforeEachTestRuns() {

        s3Uploader = createStrictMock(UploadFileToS3.class);
        dataRefreshed = createMock(RemoteDataAvailable.class);

        List<RemoteDataSourceConfig> remoteConfigs = new ArrayList<>();

        remoteConfigs.add(new DataSourceConfig(Path.of("data/xxx"), "aaa", DataSourceID.tfgm));
        remoteConfigs.add(new DataSourceConfig(Path.of("data/yyy"), "bbb", DataSourceID.rail));
        remoteConfigs.add(new DataSourceConfig(Path.of("data/zzz"), "ccc", DataSourceID.nptg));

        TramchesterConfig config = new ConfigWithRemoteSource(remoteConfigs);

        uploadRemoteData = new UploadRemoteSourceData(s3Uploader, config, dataRefreshed);
    }

    @Test
    void shouldUpdateEachRemoteDataSourceInConfigToS3() {

        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.tfgm)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.rail)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.nptg)).andReturn(true);

        EasyMock.expect(dataRefreshed.fileFor(DataSourceID.tfgm)).andReturn(Path.of("data", "tram", "fileA.zip"));
        EasyMock.expect(dataRefreshed.fileFor(DataSourceID.rail)).andReturn(Path.of("data", "bus", "fileB.zip"));
        EasyMock.expect(dataRefreshed.fileFor(DataSourceID.nptg)).andReturn(Path.of("data", "naptan", "fileC.xml"));

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

        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.tfgm)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.rail)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.nptg)).andReturn(true);

        EasyMock.expect(dataRefreshed.fileFor(DataSourceID.tfgm)).andReturn(Path.of("data", "tram", "fileA.zip"));
        EasyMock.expect(dataRefreshed.fileFor(DataSourceID.rail)).andReturn(Path.of("data", "bus", "fileB.zip"));
        //EasyMock.expect(dataRefreshed.fileFor(DataSourceID.nptg)).andReturn(Path.of("data", "naptan", "fileC.xml"));

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

    private static class DataSourceConfig extends RemoteDataSourceConfig {
        private final Path path;
        private final String filename;
        private final DataSourceID dataSourceID;

        private DataSourceConfig(Path path, String filename, DataSourceID dataSourceID) {
            this.path = path;
            this.filename = filename;
            this.dataSourceID = dataSourceID;
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
        public Duration getDefaultExpiry() {
            return Duration.ofDays(1);
        }

        @Override
        public String getDownloadFilename() {
            return filename;
        }

        @Override
        public String getName() {
            return dataSourceID.name();
        }

        @Override
        public boolean getIsS3() {
            return false;
        }
    }
}
