package com.tramchester.integration.cloud;

import com.tramchester.cloud.ClientForS3;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientForS3Test {

    private static final String TEST_BUCKET_NAME = "tramchestertestlivedatabucket";
    private static final String PREFIX = "test";
    private static final String KEY = PREFIX+"/"+"key";
    private S3Client s3;
    private ClientForS3 clientForS3;
    private S3Waiter s3Waiter;

    @BeforeEach
    void beforeEachTestRuns() {
        s3Waiter = S3Waiter.create();
        s3 = S3Client.builder().build();
        clientForS3 = new ClientForS3(TestEnv.GET());
        tidyBucket();
    }

    @AfterEach
    void afterEachTestRuns() {
        tidyBucket();
        s3Waiter.close();
        s3.close();
    }

    @Test
    void shouldUploadOkIfBucketExist() throws IOException {
        createTestBucketAndWait();
        validateUpload();
    }

    @Test
    void checkForObjectExisting() {
        createTestBucketAndWait();

        PutObjectRequest request = PutObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();
        s3.putObject(request, RequestBody.fromString("contents"));

        HeadObjectRequest objectRequest = HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();
        s3Waiter.waitUntilObjectExists(objectRequest);
        Assertions.assertTrue(clientForS3.keyExists(PREFIX, KEY), "exists"); //waiter will throw if times out

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();
        s3.deleteObject(deleteRequest);

        s3Waiter.waitUntilObjectNotExists(objectRequest);

        Assertions.assertFalse(clientForS3.keyExists(PREFIX, KEY), "deleted");

    }

    private void validateUpload() throws IOException {
        String contents = "someJsonData";
        boolean uploaded = clientForS3.upload(KEY, contents);
        Assertions.assertTrue(uploaded, "uploaded");

        // the different S3 clients involved get out of sync, so the one sees the bucket and the the other does not
        HeadBucketRequest existsRequest = HeadBucketRequest.builder().bucket(TEST_BUCKET_NAME).build();
        s3Waiter.waitUntilBucketExists(existsRequest);


        ListObjectsRequest listRequest = ListObjectsRequest.builder().bucket(TEST_BUCKET_NAME).build();
        ListObjectsResponse currentContents = s3.listObjects(listRequest);

        List<S3Object> summary = currentContents.contents();

        assertEquals(1, summary.size());
        String key = summary.get(0).key();

        assertEquals("test/key", key);

        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();

        ResponseInputStream<GetObjectResponse> theObject = s3.getObject(getRequest);

        byte[] target = new byte[contents.length()];
        theObject.read(target);

        String result = new String(target);
        assertEquals(contents, result);
    }

    void createTestBucketAndWait() {
        CreateBucketRequest createRequest = CreateBucketRequest.builder().bucket(TEST_BUCKET_NAME).build();
        s3.createBucket(createRequest);
        S3Waiter s3Waiter = S3Waiter.create();

        HeadBucketRequest bucketExists = HeadBucketRequest.builder().bucket(TEST_BUCKET_NAME).build();
        s3Waiter.waitUntilBucketExists(bucketExists);

        s3Waiter.close();
    }

    private void tidyBucket() {
        // due to eventual consistency and unrealability of tests this has become a bit belt and braces :-(

        ListBucketsResponse listBucketsResponse = s3.listBuckets();
        Set<String> buckets = listBucketsResponse.buckets().stream().map(Bucket::name).collect(Collectors.toSet());
        if (buckets.contains(TEST_BUCKET_NAME)) {

            ListObjectsRequest listRequest;
            listRequest = ListObjectsRequest.builder().bucket(TEST_BUCKET_NAME).build();
            ListObjectsResponse listObjsResponse = s3.listObjects(listRequest);
            List<S3Object> items = listObjsResponse.contents();

            if (!items.isEmpty()) {
                Set<ObjectIdentifier> toDelete = items.stream().map(item -> ObjectIdentifier.builder().key(item.key()).build()).collect(Collectors.toSet());
                Delete build = Delete.builder().objects(toDelete).quiet(false).build();
                DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder().bucket(TEST_BUCKET_NAME).delete(build).build();
                s3.deleteObjects(deleteObjectsRequest);

                items.forEach(item -> {
                    s3Waiter.waitUntilObjectNotExists(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(item.key()).build());
                });
            }

            //// BUCKET
            try {
                DeleteBucketRequest deleteRequest = DeleteBucketRequest.builder().bucket(TEST_BUCKET_NAME).build();
                s3.deleteBucket(deleteRequest);
            }
            catch (S3Exception deletefailed) {
                if (deletefailed.statusCode()==404) {
                    return;
                    // fine, no need to delete
                } else {
                    throw deletefailed;
                }
            }

            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(TEST_BUCKET_NAME).build());
            }
            catch (S3Exception possibilyAlreadyGone) {
                if (possibilyAlreadyGone.statusCode()==404) {
                    return;
                }
            }

            s3Waiter.waitUntilBucketNotExists(HeadBucketRequest.builder().bucket(TEST_BUCKET_NAME).build());
        }
    }


}
