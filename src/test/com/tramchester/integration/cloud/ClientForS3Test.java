package com.tramchester.integration.cloud;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tramchester.TestConfig;
import com.tramchester.cloud.ClientForS3;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ClientForS3Test {

    private static final String TEST_BUCKET_NAME = "tramchestertestlivedatabucket";
    private static final String PREFIX = "test";
    private static final String KEY = PREFIX+"/"+"key";
    private AmazonS3 s3;
    private ClientForS3 clientForS3;

    @Before
    public void beforeEachTestRuns() throws InterruptedException {
        s3 = AmazonS3ClientBuilder.defaultClient();
        clientForS3 = new ClientForS3(new TestConfig() {
            @Override
            public Path getDataFolder() {
                return null;

            }
        });
        tidyBucket();
    }

    @After
    public void afterEachTestRuns() throws InterruptedException {
        tidyBucket();
        s3.shutdown();
    }

    @Test
    public void shouldUploadOkIfBucketExist() throws IOException {
        s3.createBucket(TEST_BUCKET_NAME);
        validateUpload();
    }

    @Test
    public void shouldCreateBucketAndUpload() throws IOException {
        validateUpload();
    }

    @Test
    public void checkForObjectExisting() {
        s3.createBucket(TEST_BUCKET_NAME);
        s3.putObject(TEST_BUCKET_NAME, KEY, "contents");
        assertTrue(clientForS3.keyExists(PREFIX,KEY));

        s3.deleteObject(TEST_BUCKET_NAME, KEY);
        assertFalse(clientForS3.keyExists(PREFIX, KEY));
    }

    private void validateUpload() throws IOException {
        String contents = "someJsonData";
        boolean uploaded = clientForS3.upload(KEY, contents);
        assertTrue(uploaded);

        ObjectListing currentContents = s3.listObjects(TEST_BUCKET_NAME);
        List<S3ObjectSummary> summary = currentContents.getObjectSummaries();
        assertEquals(1, summary.size());
        String key = summary.get(0).getKey();
        assertEquals("test/key", key);

        S3Object theObject = s3.getObject(TEST_BUCKET_NAME, KEY);

        byte[] target = new byte[contents.length()];
        theObject.getObjectContent().read(target);

        String storedJson = new String(target);
        assertEquals(contents, storedJson);
    }

    private void tidyBucket() throws InterruptedException {
        AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        if (s3.doesBucketExistV2(TEST_BUCKET_NAME)) {
            DeleteObjectsRequest deleteObjects = new DeleteObjectsRequest(TEST_BUCKET_NAME).withKeys(KEY);
            s3.deleteObjects(deleteObjects);
            s3.deleteBucket(TEST_BUCKET_NAME);
        }
        while (s3.doesBucketExistV2(TEST_BUCKET_NAME)) {
            Thread.sleep(100);
        }
    }
}
