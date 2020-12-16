package com.tramchester.cloud.data;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

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
    private final String bucket;

    private S3Client s3Client;

    @Inject
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
        logger.info("Downloading data for " + keys.size() + " keys");
        return keys.parallelStream().map(key -> download(key, responseMapper)).flatMap(Collection::stream);
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

    @NotNull
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

    private List<S3Object> getSummaryForPrefix(String prefix) {
        List<S3Object> results = new ArrayList<>();
        ListObjectsV2Response response;
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix);
        try {
            // in the try block , exists can throw S3Exception
            if (!bucketExists(bucket)) {
                logger.error(format("Bucket %s does not exist", bucket));
                return Collections.emptyList();
            }

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
