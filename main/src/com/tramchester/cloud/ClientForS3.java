package com.tramchester.cloud;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tramchester.config.TramchesterConfig;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.lang.String.format;

public class ClientForS3 {
    private static final Logger logger = LoggerFactory.getLogger(ClientForS3.class);

    private final TramchesterConfig config;
    private AmazonS3 s3Client;

    public ClientForS3(TramchesterConfig config) {
        this.config = config;
        try {
            s3Client = AmazonS3ClientBuilder.defaultClient();
        }
        catch (com.amazonaws.SdkClientException exception) {
            logger.warn("Unable to init S3 client, no live data will be archived.");
        }
    }

    public boolean upload(String key, String json) {
        String bucket = config.getLiveDataS3Bucket();
        logger.debug(format("Uploading to bucket '%s' key '%s' contents '%s'", bucket, key, json));

        try {
            if (!s3Client.doesBucketExistV2(bucket)) {
                logger.warn(format("Bucket %s does not exist", bucket));
                return false;
            }

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            String localMd5 = Base64.encodeBase64String(messageDigest.digest(json.getBytes()));

            logger.debug("Uploading with MD5: " + localMd5);
            PutObjectResult result = s3Client.putObject(bucket, key, json);

            String remoteMd5 = result.getContentMd5();
            if (localMd5.equals(remoteMd5)) {
                return true;
            }

            logger.warn(format("Problem with upload, md5 mismatch. expected '%s' got '%s' ", localMd5, remoteMd5));
        }
        catch (AmazonS3Exception | NoSuchAlgorithmException exception) {
            logger.warn(format("Unable to upload to bucket '%s' key '%s'", bucket, key), exception);
        }
        return false;
    }

    public boolean keyExists(String prefix, String key) {
        String bucket = config.getLiveDataS3Bucket();
        try {
            if (!s3Client.doesBucketExistV2(bucket)) {
                logger.error(format("Bucket %s does not exist", bucket));
                return false;
            }
            // limit by prefix to avoid very large requests
            ObjectListing summary = s3Client.listObjects(bucket, prefix);
            for (S3ObjectSummary item : summary.getObjectSummaries()) {
                if (key.equals(item.getKey())) {
                    return true;
                }
            }
        }
        catch (AmazonS3Exception exception) {
            logger.warn(format("Cannot check if key '%s' exists in bucket '%s' reason '%s'",
                    key, bucket, exception.getMessage()));
        }
        logger.warn(format("Could not find %s in bucket %s", key, bucket));
        return false;
    }

    public boolean isStarted() {
        return s3Client!=null;
    }
}
