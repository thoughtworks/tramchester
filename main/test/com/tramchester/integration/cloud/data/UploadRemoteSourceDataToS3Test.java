package com.tramchester.integration.cloud.data;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.deployment.UploadRemoteSourceData;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.S3Test;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@S3Test
class UploadRemoteSourceDataToS3Test {

    private static S3Client s3;
    private static S3Waiter s3Waiter;
    private static S3TestSupport s3TestSupport;

    private static GuiceContainerDependencies componentContainer;
    private static UploadRemoteSourceData uploadRemoteData;

    private final static String TEST_BUCKET_NAME = "tramchestertestlivedatabucket";
    private static DataSource dataSource;

    @BeforeAll
    static void beforeAnyDone() {

        dataSource = new DataSource();
        TramchesterConfig configuration = new IntegrationTestBucketConfig(TEST_BUCKET_NAME, dataSource);
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        s3 = S3Client.builder().build();
        s3Waiter = S3Waiter.create();

        s3TestSupport = new S3TestSupport(s3, s3Waiter, configuration.getDistributionBucket());
        s3TestSupport.createOrCleanBucket();

        uploadRemoteData = componentContainer.get(UploadRemoteSourceData.class);
    }

    @AfterAll
    static void afterAllDone() {
        componentContainer.close();

        s3TestSupport.deleteBucket();
        s3Waiter.close();
        s3.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        s3TestSupport.cleanBucket();
    }

    @AfterEach
    void afterEachTestRuns() {
        s3TestSupport.cleanBucket();
    }

    @Test
    void shouldUploadOkIfBucketExist() throws IOException {

        // Can't check actual remote URL, so make sure the files is not "expired" by creating a new file

        Path sourceFilePath = dataSource.getDataPath().resolve(dataSource.getDownloadFilename());

        Files.deleteIfExists(sourceFilePath);

        final String text = "HereIsSomeTextForTheFile";
        Files.writeString( sourceFilePath, text);

        final String testPrefix = "testing/testPrefix";
        boolean result = uploadRemoteData.upload(testPrefix);
        assertTrue(result);

        ListObjectsRequest listRequest = ListObjectsRequest.builder().bucket(TEST_BUCKET_NAME).build();
        ListObjectsResponse currentContents = s3.listObjects(listRequest);

        List<S3Object> summary = currentContents.contents();

        assertEquals(1, summary.size());
        String foundKey = summary.get(0).key();

        final String key = testPrefix +"/"+ dataSource.getDownloadFilename();
        assertEquals(key, foundKey);

        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).build();
        ResponseInputStream<GetObjectResponse> inputStream = s3.getObject(getRequest);

        byte[] buffer = new byte[text.length()];
        int readSize = inputStream.read(buffer);
        inputStream.close();

        String remoteContents = new String(buffer);
        assertEquals(remoteContents, text);
        assertEquals(readSize, text.length());

        boolean repeatedUpload = uploadRemoteData.upload(testPrefix);
        assertTrue(repeatedUpload, "overwrite failed");
    }

    public static class IntegrationTestBucketConfig extends TestConfig {

        private final String bucketName;
        private final RemoteDataSourceConfig dataSource;

        public IntegrationTestBucketConfig(String bucketName, RemoteDataSourceConfig dataSource) {
            this.bucketName = bucketName;
            this.dataSource = dataSource;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public String getDistributionBucket() {
            return bucketName;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(dataSource);
        }
    }

    public static class DataSource implements RemoteDataSourceConfig {

        @Override
        public Path getDataPath() {
            return Path.of("data", "test");
        }

        @Override
        public String getDataCheckUrl() {
            return "";
        }

        @Override
        public String getDataUrl() {
            throw new RuntimeException("Should not be downloading, not expired");
        }

        @Override
        public String getDownloadFilename() {
            return "testFile.txt";
        }

        @Override
        public String getName() {
            return "tfgm";
        }

        @Override
        public boolean getIsS3() {
            return false;
        }
    }



}
