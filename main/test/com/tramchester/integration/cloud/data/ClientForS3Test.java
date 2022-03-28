package com.tramchester.integration.cloud.data;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

public class ClientForS3Test {
    private static S3Client awsS3;
    private static S3Waiter s3Waiter;
    private static S3TestSupport s3TestSupport;

    private static GuiceContainerDependencies componentContainer;

    private final static String BUCKET = "tramchestertestlivedatabucket";
    private static ClientForS3 clientForS3;
    private final Path testFilePath = Path.of("testFile.txt");

    @BeforeAll
    static void beforeAnyDone() {

        TramchesterConfig configuration = new TestBucketConfig(BUCKET);
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        awsS3 = S3Client.builder().build();
        s3Waiter = S3Waiter.create();

        s3TestSupport = new S3TestSupport(awsS3, s3Waiter, configuration.getDistributionBucket());
        s3TestSupport.createOrCleanBucket();

        clientForS3 = componentContainer.get(ClientForS3.class);
    }

    @AfterAll
    static void afterAllDone() {
        componentContainer.close();

        //s3TestSupport.deleteBucket();
        s3Waiter.close();
        awsS3.close();
    }

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
        s3TestSupport.cleanBucket();
    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
        s3TestSupport.cleanBucket();
    }

    @Test
    void shouldCreateThenDeleteAKey() throws IOException {

        final String fullKey = "test/key";
        final String text = "someTextToPlaceInS3";
        clientForS3.upload(BUCKET, fullKey, text);

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // exists?
        assertTrue(clientForS3.keyExists(BUCKET, "test", "key"));

        // get contents
        String resultText = getContentsOfKey(fullKey);
        assertEquals(text, resultText);

        // delete
        awsS3.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET).key(fullKey).build());
        s3Waiter.waitUntilObjectNotExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // not exists?
        assertFalse(clientForS3.keyExists(BUCKET, "test", "key"));
    }

    @Test
    void shouldUploadFromFile() throws IOException {

        final String text = "someTextInAFileToUploadToS3";
        Files.writeString(testFilePath, text);

        final String fullKey = "test/file";

        clientForS3.upload(BUCKET, fullKey, testFilePath);

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // exists
        assertTrue(clientForS3.keyExists(BUCKET, "test", "file"));

        // get contents
        String resultText = getContentsOfKey(fullKey);
        assertEquals(text, resultText);

        // delete
        awsS3.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET).key(fullKey).build());
        s3Waiter.waitUntilObjectNotExists(HeadObjectRequest.builder().bucket(BUCKET).key(fullKey).build());

        // not exists?
        assertFalse(clientForS3.keyExists(BUCKET, "test", "key"));
    }

    @Test
    void shouldGetKeysFor() {

        Set<String> result = clientForS3.getKeysFor(BUCKET, "test");
        assertTrue(result.isEmpty());

        String keyA = "test/keyA";
        String keyB = "test/keyB";
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(keyA).build(), RequestBody.fromString("text1"));
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(keyB).build(), RequestBody.fromString("text2"));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(keyA).build());
        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(keyB).build());

        result = clientForS3.getKeysFor(BUCKET, "test");
        assertEquals(2, result.size());

        assertTrue(result.contains(keyA));
        assertTrue(result.contains(keyB));
    }

    @Test
    void shouldGetModTime() throws IOException {
        String key = "test/keyModTime";

        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(key).build(), RequestBody.fromString("text1"));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        ResponseInputStream<GetObjectResponse> stream = awsS3.getObject(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build());
        Instant instantFromS3 = stream.response().lastModified();
        ZonedDateTime dateTimeFromS3 = instantFromS3.atZone(TramchesterConfig.TimeZoneId);
        stream.close();

        LocalDateTime modTime = clientForS3.getModTimeFor(format("s3://%s/%s", BUCKET, key));

        assertEquals(dateTimeFromS3.toLocalDateTime(), modTime);
    }

    @Test
    void shouldDownloadAndMap() {
        String key = "test/keyDownloadAndMap";

        final String text = "testToUpdateForMapper";
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(key).build(), RequestBody.fromString(text));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        List<String> result = clientForS3.downloadAndMapForKey(BUCKET, key, bytes -> Collections.singletonList(new String(bytes)));

        assertEquals(1, result.size());
        assertTrue(result.contains(text));
    }

    @Test
    void shouldDownloadToFile() throws IOException {
        String key = "test/keyDownloadAndSave";

        final String text = "textToDownloadToAFile";
        awsS3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(key).build(), RequestBody.fromString(text));

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        clientForS3.downloadTo(testFilePath, format("s3://%s/%s", BUCKET, key));

        assertTrue(Files.exists(testFilePath));

        byte[] bytes = Files.readAllBytes(testFilePath);
        String result = new String(bytes);
        assertEquals(text, result);
    }

    @Test
    void shouldUploadWithZip() throws IOException {
        String key = "test/keyZipAndUpload";

        final String text = "someTextToUploadThatShouldEndUpZipped";
        Files.writeString(testFilePath, text);

        boolean ok = clientForS3.uploadZipped(BUCKET, key, testFilePath);
        assertTrue(ok);

        s3Waiter.waitUntilObjectExists(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());

        ResponseInputStream<GetObjectResponse> result = awsS3.getObject(GetObjectRequest.builder().
                bucket(BUCKET).
                key(key).build());
        byte[] bytes = result.readAllBytes();

        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));

        ZipEntry entry = zipInputStream.getNextEntry();
        assertNotNull(entry);
        assertEquals(testFilePath.getFileName().toString(), entry.getName());

        byte[] outputBuffer = new byte[text.length()];
        int read = zipInputStream.read(outputBuffer, 0, text.length());
        zipInputStream.closeEntry();
        zipInputStream.close();

        assertEquals(text.length(), read);
        assertEquals(text, new String(outputBuffer));


    }

    @NotNull
    private String getContentsOfKey(String key) throws IOException {
        ResponseInputStream<GetObjectResponse> result = awsS3.getObject(GetObjectRequest.builder().
                bucket(BUCKET).
                key(key).build());
        byte[] bytes = result.readAllBytes();
        return new String(bytes);
    }

    public static class TestBucketConfig extends TestConfig {

        private final String bucketName;

        public TestBucketConfig(String bucketName) {
            this.bucketName = bucketName;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public String getDistributionBucket() {
            return bucketName;
        }

    }

}
