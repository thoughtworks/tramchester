package com.tramchester.integration.cloud.data;

import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientForS3Test {

    private static final String TEST_BUCKET_NAME = "tramchestertestlivedatabucket";
    private static final String PREFIX = "test";
    private static final String KEY = PREFIX+"/"+"key";

    private static S3Client s3;
    private static S3Waiter s3Waiter;

    private ClientForS3 clientForS3;

    @BeforeAll
    static void beforeAnyDone() {
        s3 = S3Client.builder().build();
        s3Waiter = S3Waiter.create();

        ListBucketsResponse buckets = s3.listBuckets();
        boolean present = buckets.buckets().stream().anyMatch(bucket -> bucket.name().equals(TEST_BUCKET_NAME));

        if (present) {
            cleanBucket();
        } else {
            CreateBucketRequest createRequest = CreateBucketRequest.builder().bucket(TEST_BUCKET_NAME).build();
            s3.createBucket(createRequest);
        }

        HeadBucketRequest bucketExists = HeadBucketRequest.builder().bucket(TEST_BUCKET_NAME).build();
        s3Waiter.waitUntilBucketExists(bucketExists);
    }

    @AfterAll
    static void afterAllDone() {
        deleteBucket();
        s3Waiter.close();
        s3.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        clientForS3 = new ClientForS3(TestEnv.GET());
        cleanBucket();
    }

    @AfterEach
    void afterEachTestRuns() {
        cleanBucket();
    }

    @Test
    void shouldUploadOkIfBucketExist() throws IOException {

        String contents = "someJsonData";
        boolean uploaded = clientForS3.upload(KEY, contents);
        assertTrue(uploaded, "uploaded");

        ListObjectsRequest listRequest = ListObjectsRequest.builder().bucket(TEST_BUCKET_NAME).build();
        ListObjectsResponse currentContents = s3.listObjects(listRequest);

        List<S3Object> summary = currentContents.contents();

        assertEquals(1, summary.size());
        String key = summary.get(0).key();

        assertEquals("test/key", key);

        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();
        ResponseInputStream<GetObjectResponse> inputStream = s3.getObject(getRequest);

        byte[] target = new byte[contents.length()];
        inputStream.read(target);
        inputStream.close();

        String result = new String(target);
        assertEquals(contents, result);
    }

    @Test
    void checkForObjectExisting() {
        HeadObjectRequest existsCheckRequest = HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();

        PutObjectRequest request = PutObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();
        s3.putObject(request, RequestBody.fromString("contents"));
        s3Waiter.waitUntilObjectExists(existsCheckRequest);

        assertTrue(clientForS3.keyExists(PREFIX, KEY), "exists"); //waiter will throw if times out
        Set<String> keys = clientForS3.getKeysFor(PREFIX);
        assertTrue(keys.contains(KEY));

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(KEY).build();
        s3.deleteObject(deleteRequest);
        s3Waiter.waitUntilObjectNotExists(existsCheckRequest);

        Assertions.assertFalse(clientForS3.keyExists(PREFIX, KEY), "deleted");
    }

    @Test
    void shouldDownloadFromBucket() throws NoSuchAlgorithmException {
        String payload = "contents";
        String key = KEY + "B";
        HeadObjectRequest existsCheckRequest = HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).build();

        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] bytes = payload.getBytes();
        String localMd5 = Base64.encodeBase64String(messageDigest.digest(bytes));

        PutObjectRequest request = PutObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(key).contentMD5(localMd5).build();
        s3.putObject(request, RequestBody.fromString(payload));
        s3Waiter.waitUntilObjectExists(existsCheckRequest);

        String result = clientForS3.download(key);
        assertEquals(payload, result);
    }

    static void cleanBucket() {
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
    }

    private static void deleteBucket() {
        // due to eventual consistency and unrealability of tests this has become a bit belt and braces :-(

        ListBucketsResponse listBucketsResponse = s3.listBuckets();
        Set<String> buckets = listBucketsResponse.buckets().stream().map(Bucket::name).collect(Collectors.toSet());
        if (buckets.contains(TEST_BUCKET_NAME)) {

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
