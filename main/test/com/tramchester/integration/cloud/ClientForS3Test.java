package com.tramchester.integration.cloud;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.cloud.ClientForS3;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
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
    public void beforeEachTestRuns() {
        s3 = AmazonS3ClientBuilder.defaultClient();
        clientForS3 = new ClientForS3(TestConfig.GET());
        tidyBucket();
    }

    @After
    public void afterEachTestRuns() {
        tidyBucket();
        s3.shutdown();
    }

    @Test
    public void shouldUploadOkIfBucketExist() throws IOException {
        createTestBucketAndWait();
        validateUpload();
    }

    @Test
    public void checkForObjectExisting() {
        createTestBucketAndWait();
        s3.putObject(TEST_BUCKET_NAME, KEY, "contents");
        s3.waiters().objectExists().run(new WaiterParameters<>(new GetObjectMetadataRequest(TEST_BUCKET_NAME, KEY)));
        assertTrue("exists", clientForS3.keyExists(PREFIX, KEY)); //waiter will throw if times out

        s3.deleteObject(TEST_BUCKET_NAME, KEY);
        s3.waiters().objectNotExists().run(new WaiterParameters<>(new GetObjectMetadataRequest(TEST_BUCKET_NAME, KEY)));
        assertFalse("deleted",clientForS3.keyExists(PREFIX, KEY));
    }

    private void validateUpload() throws IOException {
        String contents = "someJsonData";
        boolean uploaded = clientForS3.upload(KEY, contents);
        assertTrue("uploaded",uploaded);

        // the different S3 clients involved get out of sync, so the one sees the bucket and the the other does not
        Waiter<HeadBucketRequest> waiter = s3.waiters().bucketExists();
        waiter.run(new WaiterParameters<>(new HeadBucketRequest(TEST_BUCKET_NAME)));

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

    public void createTestBucketAndWait() {
        s3.createBucket(TEST_BUCKET_NAME);
        Waiter<HeadBucketRequest> waiter = s3.waiters().bucketExists();
        waiter.run(new WaiterParameters<>(new HeadBucketRequest(TEST_BUCKET_NAME)));
    }

    private void tidyBucket() {
        // due to eventual consistency and unrealability of tests this has become a bit belt and braces :-(
        if (s3.doesBucketExistV2(TEST_BUCKET_NAME)) {

            ObjectListing listing = s3.listObjects(TEST_BUCKET_NAME);
            List<S3ObjectSummary> items = listing.getObjectSummaries();

            items.forEach(item -> {
                String key = item.getKey();
                DeleteObjectsRequest deleteObjects = new DeleteObjectsRequest(TEST_BUCKET_NAME).withKeys(key);
                try {
                    s3.deleteObjects(deleteObjects);
                    Waiter<GetObjectMetadataRequest> keyWaiter = s3.waiters().objectNotExists();
                    keyWaiter.run(new WaiterParameters<>(new GetObjectMetadataRequest(TEST_BUCKET_NAME, key)));
                }
                catch (AmazonS3Exception deletefailed) {
                    if (deletefailed.getStatusCode()==404) {
                        // fine, no need to delete
                    } else {
                        throw deletefailed;
                    }
                }
            });

            //// KEY
//            DeleteObjectsRequest deleteObjects = new DeleteObjectsRequest(TEST_BUCKET_NAME).withKeys(KEY);

//            Waiter<GetObjectMetadataRequest> keyWaiter = s3.waiters().objectNotExists();
//            keyWaiter.run(new WaiterParameters<>(new GetObjectMetadataRequest(TEST_BUCKET_NAME, KEY)));

            //// BUCKET
            try {
                s3.deleteBucket(TEST_BUCKET_NAME);
            }
            catch (AmazonS3Exception deletefailed) {
                if (deletefailed.getStatusCode()==404) {
                    // fine, no need to delete
                } else {
                    throw deletefailed;
                }
            }

            Waiter<HeadBucketRequest> bucketWaiter = s3.waiters().bucketNotExists();
            WaiterParameters<HeadBucketRequest> waiterParameters = new WaiterParameters<>(new HeadBucketRequest(TEST_BUCKET_NAME));
            bucketWaiter.run(waiterParameters);
        }
    }


}
