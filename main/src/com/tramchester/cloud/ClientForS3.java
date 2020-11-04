package com.tramchester.cloud;

import com.tramchester.config.TramchesterConfig;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ClientForS3 {
    private static final Logger logger = LoggerFactory.getLogger(ClientForS3.class);

    private final TramchesterConfig config;
    private S3Client s3Client;

    public ClientForS3(TramchesterConfig config) {
        this.config = config;
        try {
            s3Client = S3Client.create();
        }
        catch (com.amazonaws.SdkClientException exception) {
            logger.warn("Unable to init S3 client, no live data will be archived.");
        }
    }

    public boolean upload(String key, String json) {
        String bucket = config.getLiveDataS3Bucket();
        logger.debug(format("Uploading to bucket '%s' key '%s' contents '%s'", bucket, key, json));

        try {
            if (!bucketExists(bucket)) {
                logger.warn(format("Bucket %s does not exist", bucket));
                return false;
            }

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] bytes = json.getBytes();
            String localMd5 = Base64.encodeBase64String(messageDigest.digest(bytes));

            logger.debug("Uploading with MD5: " + localMd5);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(key).contentMD5(localMd5).build(); ;
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
        }
        catch (S3Exception | NoSuchAlgorithmException exception) {
            logger.warn(format("Unable to upload to bucket '%s' key '%s'", bucket, key), exception);
            return false;
        }
        return true;
    }

    private boolean bucketExists(String bucket) {
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        Set<String> buckets = listBucketsResponse.buckets().stream().map(Bucket::name).collect(Collectors.toSet());
        return buckets.contains(bucket);
    }

    public boolean keyExists(String prefix, String key) {
        String bucket = config.getLiveDataS3Bucket();
        try {
            if (!bucketExists(bucket)) {
                logger.error(format("Bucket %s does not exist", bucket));
                return false;
            }
            // limit by prefix to avoid very large requests
            ListObjectsRequest listObsRequest = ListObjectsRequest.builder().bucket(bucket).prefix(prefix).build();
            ListObjectsResponse summary = s3Client.listObjects(listObsRequest);

            for (S3Object item : summary.contents()) {
                if (key.equals(item.key())) {
                    return true;
                }
            }
        }
        catch (S3Exception exception) {
            logger.warn(format("Cannot check if key '%s' exists in bucket '%s' reason '%s'",
                    key, bucket, exception.getMessage()));
        }
        return false;
    }

    public boolean isStarted() {
        return s3Client!=null;
    }
}
