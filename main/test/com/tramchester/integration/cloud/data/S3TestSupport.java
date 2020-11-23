package com.tramchester.integration.cloud.data;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class S3TestSupport {

    private final S3Client s3;
    private final S3Waiter s3Waiter;
    private final String bucketName;

    public S3TestSupport(S3Client s3, S3Waiter s3Waiter, String bucketName) {
        this.s3 = s3;
        this.s3Waiter = s3Waiter;
        this.bucketName = bucketName;
    }

    void createOrCleanBucket() {
        ListBucketsResponse buckets = s3.listBuckets();
        boolean present = buckets.buckets().stream().anyMatch(bucket -> bucket.name().equals(bucketName));

        if (present) {
            cleanBucket();
        } else {
            CreateBucketRequest createRequest = CreateBucketRequest.builder().bucket(bucketName).build();
            s3.createBucket(createRequest);
        }

        HeadBucketRequest bucketExists = HeadBucketRequest.builder().bucket(bucketName).build();
        s3Waiter.waitUntilBucketExists(bucketExists);
    }

    void deleteBucket() {
        // due to eventual consistency and unrealability of tests this has become a bit belt and braces :-(

        ListBucketsResponse listBucketsResponse = s3.listBuckets();
        Set<String> buckets = listBucketsResponse.buckets().stream().map(Bucket::name).collect(Collectors.toSet());
        if (buckets.contains(bucketName)) {

            try {
                DeleteBucketRequest deleteRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
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
                s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            }
            catch (S3Exception possibilyAlreadyGone) {
                if (possibilyAlreadyGone.statusCode()==404) {
                    return;
                }
            }

            s3Waiter.waitUntilBucketNotExists(HeadBucketRequest.builder().bucket(bucketName).build());
        }
    }

    void cleanBucket() {
        ListObjectsRequest listRequest;
        listRequest = ListObjectsRequest.builder().bucket(bucketName).build();
        ListObjectsResponse listObjsResponse = s3.listObjects(listRequest);
        List<S3Object> items = listObjsResponse.contents();

        if (!items.isEmpty()) {
            Set<ObjectIdentifier> toDelete = items.stream().map(item -> ObjectIdentifier.builder().key(item.key()).build()).collect(Collectors.toSet());
            Delete build = Delete.builder().objects(toDelete).quiet(false).build();
            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder().bucket(bucketName).delete(build).build();
            s3.deleteObjects(deleteObjectsRequest);

            items.forEach(item -> s3Waiter.waitUntilObjectNotExists(HeadObjectRequest.builder().bucket(bucketName).key(item.key()).build()));
        }
    }
}
