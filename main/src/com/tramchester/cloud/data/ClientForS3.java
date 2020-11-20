package com.tramchester.cloud.data;

import com.tramchester.config.TramchesterConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ClientForS3 {

    // TODO Switch to specific string encoding to/from bytes instead of the default

    private static final Logger logger = LoggerFactory.getLogger(ClientForS3.class);
    private final String bucket;

    private S3Client s3Client;

    public ClientForS3(TramchesterConfig config) {
        this.bucket = config.getLiveDataConfig().getS3Bucket();
        try {
            s3Client = S3Client.create();
        }
        catch (AwsServiceException exception) {
            logger.warn("Unable to init S3 client, no live data will be archived.");
        }
    }

    public boolean upload(String key, String json) {
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

    public String download(String key) {
        logger.info("Download from bucket " + bucket + " with key " +key);
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
        ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(request);

        GetObjectResponse response = inputStream.response();
        logger.info("Content type: " + response.contentType());
        Long contentLength = response.contentLength();
        logger.info("Content length: " + contentLength);

        byte[] bytes = new byte[Math.toIntExact(contentLength)];

        try {
            inputStream.read(bytes);

            String remote = getETagClean(response);
            String localMd5 = DigestUtils.md5Hex(bytes);
            if (!localMd5.equals(remote)) {
                logger.error(format("MD5 mismatch downloading from bucket %s key %s expected '%s' got '%s'",
                        bucket, key, localMd5, remote));
            } else {
                logger.info(format("MD5 match for %s key %s md5: '%s'", bucket, key, localMd5));
            }

            inputStream.close();

        } catch (IOException exception) {
            logger.error("Exception downloading from bucket " + bucket + " with key " +key, exception);
        }

        return new String(bytes);
    }

    @NotNull
    public String getETagClean(GetObjectResponse response) {
        String remote = response.eTag();
        if (remote.startsWith("\"")) {
            remote = remote.replaceFirst("\"","");
        }
        if (remote.endsWith("\"")) {
            remote = remote.substring(0, remote.length()-1);
        }
        return remote;
    }

    private boolean bucketExists(String bucket) {
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        Set<String> buckets = listBucketsResponse.buckets().stream().map(Bucket::name).collect(Collectors.toSet());
        return buckets.contains(bucket);
    }

    public boolean keyExists(String prefix, String key) {
        List<S3Object> items = getSummaryForPrefix(prefix);

        for (S3Object item : items) {
            if (key.equals(item.key())) {
                return true;
            }
        }

        return false;
    }

    public Set<String> getKeysFor(String prefix) {
        List<S3Object> items = getSummaryForPrefix(prefix);
        return items.stream().map(S3Object::key).collect(Collectors.toSet());
    }

    public List<S3Object> getSummaryForPrefix(String prefix) {
        if (!bucketExists(bucket)) {
            logger.error(format("Bucket %s does not exist", bucket));
            return Collections.emptyList();
        }

        ListObjectsResponse summary;
        try {
            ListObjectsRequest listObsRequest = ListObjectsRequest.builder().bucket(bucket).prefix(prefix).build();
            summary = s3Client.listObjects(listObsRequest);
        }
        catch (S3Exception exception) {
            logger.warn(format("Cannot get objects for prefix '%s' exists in bucket '%s' reason '%s'",
                    prefix, bucket, exception.getMessage()));
            return Collections.emptyList();
        }

        if (summary==null) {
            logger.warn(format("Null answer getting objects for prefix '%s' exists in bucket '%s'",
                    prefix, bucket));
            return Collections.emptyList();
        }

        List<S3Object> contents = summary.contents();
        logger.info("Got " + contents.size() + " keys for prefix " + prefix);

        return contents;
    }

    public boolean isStarted() {
        return s3Client!=null;
    }

}
