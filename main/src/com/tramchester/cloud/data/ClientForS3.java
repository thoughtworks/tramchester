package com.tramchester.cloud.data;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.FilterInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class ClientForS3 {

    // TODO Switch to specific string encoding to/from bytes instead of the default

    private static final Logger logger = LoggerFactory.getLogger(ClientForS3.class);
    private final LiveDataConfig config;

    private String bucket;
    private S3Client s3Client;

    @Inject
    public ClientForS3(TramchesterConfig config) {
        s3Client = null;
        this.config = config.getLiveDataConfig();
    }

    @PostConstruct
    public void start() {
        if (config==null) {
            logger.warn("Not started, live data config not set");
            return;
        }

        logger.info("Starting");
        this.bucket = config.getS3Bucket();
        try {
            s3Client = S3Client.create();
            logger.info("Started");
        }
        catch (AwsServiceException | SdkClientException exception) {
            logger.error("Unable to init S3 client, no live data will be archived.", exception);
        }
    }

    @PreDestroy
    public void stop() {
        if (s3Client!=null) {
            logger.info("Stopping");
            s3Client.close();
            s3Client = null;
        }
    }

    public boolean upload(String key, String json) {
        if (!isStarted()) {
            logger.error("not started");
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(format("Uploading to bucket '%s' key '%s' contents '%s'", bucket, key, json));
        } else {
            logger.info(format("Uploading to bucket '%s' key '%s'", bucket, key));
        }

        if (!bucketExists(bucket)) {
            logger.error(format("Bucket %s does not exist", bucket));
            return false;
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] bytes = json.getBytes();
            String localMd5 = Base64.encodeBase64String(messageDigest.digest(bytes));

            logger.debug("Uploading with MD5: " + localMd5);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(key).contentMD5(localMd5).build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
        }
        catch (NoSuchAlgorithmException exception) {
            logger.warn(format("NoSuchAlgorithmException for upload to bucket '%s' key '%s'", bucket, key), exception);
            return false;
        }
        catch (AwsServiceException awsServiceException) {
            logger.error(format("AWS exception during upload for upload to bucket '%s' key '%s'", bucket, key), awsServiceException);
            return false;
        }
        return true;
    }

    public <T> Stream<T> download(Set<String> keys, ResponseMapper<T> responseMapper) {
        if (!isStarted()) {
            logger.error("not started");
            return Stream.empty();
        }

        logger.info("Downloading data for " + keys.size() + " keys");
        final Stream<String> stream = keys.parallelStream();
        stream.onClose(() -> logger.info("Download stream closed"));
        return stream.map(key -> download(key, responseMapper)).flatMap(Collection::stream);
    }

    private <T> List<T> download(String key, ResponseMapper<T> responseMapper) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();

        ResponseTransformer<GetObjectResponse, List<T>> transformer =
                (response, inputStream) -> responseMapper.map(readBytes(key, response, inputStream));

        return s3Client.getObject(request, transformer);
    }

    private byte[] readBytes(String key, GetObjectResponse response, FilterInputStream inputStream) {
        int contentLength = Math.toIntExact(response.contentLength());

        logger.info(format("Key: %s Content type: %s Length %s ", key, response.contentType(), contentLength));
        byte[] bytes = new byte[contentLength];
        int offset = 0;
        int read = 0;
        try {
            do {
                read = inputStream.read(bytes, offset, contentLength-offset);
                offset = offset + read;
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Key %s Read %s of %s bytes", key, read, contentLength));
                }
            } while (read>0);
            inputStream.close();

            checkMD5(key, response, bytes);

        } catch (IOException exception) {
            logger.error("Exception downloading from bucket " + bucket + " with key " + key, exception);
        }
        return bytes;
    }

    private void checkMD5(String key, GetObjectResponse response, byte[] bytes) {
        String remote = getETagClean(response);
        String localMd5 = DigestUtils.md5Hex(bytes);
        if (!localMd5.equals(remote)) {
            logger.error(format("MD5 mismatch downloading from bucket %s key %s local '%s' remote '%s'",
                    bucket, key, localMd5, remote));
        } else if (logger.isDebugEnabled()) {
            logger.debug(format("MD5 match for %s key %s md5: '%s'", bucket, key, localMd5));
        }
    }

    private String getETagClean(GetObjectResponse response) {
        String remote = response.eTag();
        if (remote.startsWith("\"")) {
            remote = remote.replaceFirst("\"","");
        }
        if (remote.endsWith("\"")) {
            remote = remote.substring(0, remote.length()-1);
        }
        return remote;
    }

    // NOTE: listing all buckets requires permissions beyond just the one bucket,
    // so here do an op on the bucket and catch exception instead
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean bucketExists(String bucket) {
        if (!isStarted()) {
            logger.error("not started");
            return false;
        }

        try {
            GetBucketLocationRequest request = GetBucketLocationRequest.builder().bucket(bucket).build();
            s3Client.getBucketLocation(request);
        }
        catch (AwsServiceException exception) {
            if (exception.awsErrorDetails().errorCode().equals("NoSuchBucket")) {
                logger.info("Bucket " + bucket + " not found");
            } else {
                logger.error("Could not check for existence of bucket " + bucket, exception);
            }
            return false;
        }
        return true;
    }

    public boolean keyExists(String prefix, String key) {
        if (!isStarted()) {
            logger.error("not started");
            return false;
        }

        List<S3Object> items = getSummaryForPrefix(prefix);

        for (S3Object item : items) {
            if (key.equals(item.key())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getKeysFor(String prefix) {
        if (!isStarted()) {
            logger.error("not started");
            return Collections.emptySet();
        }

        List<S3Object> items = getSummaryForPrefix(prefix);
        return items.stream().map(S3Object::key).collect(Collectors.toSet());
    }

    private List<S3Object> getSummaryForPrefix(String prefix) {
        if (!bucketExists(bucket)) {
            logger.error(format("Bucket %s does not exist so cannot get summary", bucket));
            return Collections.emptyList();
        }

        List<S3Object> results = new ArrayList<>();
        ListObjectsV2Response response;
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix);
        try {
            do {
                ListObjectsV2Request listObsRequest = builder.build();
                response = s3Client.listObjectsV2(listObsRequest);
                results.addAll(response.contents());
                String continueToken = response.nextContinuationToken();
                builder.continuationToken(continueToken);
            } while (response.isTruncated());
        }
        catch (S3Exception exception) {
            logger.warn(format("Cannot get objects for prefix '%s' exists in bucket '%s' reason '%s'",
                    prefix, bucket, exception.getMessage()), exception);
            return Collections.emptyList();
        }

        logger.info("Got " + results.size() + " keys for prefix " + prefix);

        return results;
    }

    public boolean isStarted() {
        return s3Client!=null;
    }

    public interface ResponseMapper<T> {
        List<T> map(byte[] bytes);
    }

}
