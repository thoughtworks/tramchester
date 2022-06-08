package com.tramchester.cloud.data;

import com.google.common.io.ByteStreams;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.emf.common.util.URI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;

@LazySingleton
public class ClientForS3 {
    private static final Logger logger = LoggerFactory.getLogger(ClientForS3.class);

    protected S3Client s3Client;
    MessageDigest messageDigest;


    @Inject
    public ClientForS3() {
        s3Client = null;
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            s3Client = S3Client.create();
            logger.info("Started");
        } catch (AwsServiceException | SdkClientException exception) {
            logger.error("Unable to init S3 client", exception);
        } catch (NoSuchAlgorithmException exception) {
            logger.error("Unable to file algo for message digest");
        }
    }

    @PreDestroy
    public void stop() {
        if (s3Client != null) {
            logger.info("Stopping");
            s3Client.close();
            s3Client = null;
        }
    }

    public boolean upload(String bucket, String key, Path fileToUpload) {
        logger.info(format("Uploading to bucket '%s' key '%s' file '%s'", bucket, key, fileToUpload.toAbsolutePath()));

        try {
            byte[] buffer = Files.readAllBytes(fileToUpload);
            String localMd5 = Base64.encodeBase64String(messageDigest.digest(buffer));
            return uploadToS3(bucket, key, localMd5, RequestBody.fromBytes(buffer));

        } catch (IOException e) {
           logger.info("Unable to upload file " + fileToUpload.toAbsolutePath(), e);
           return false;
        }
    }

    public boolean uploadZipped(String bucket, String key, Path uploadFile) {
        logger.info(format("Upload %s zipped to bucket:%s key:%s", uploadFile, bucket, key));
        long originalSize = uploadFile.toFile().length();

        String entryName = uploadFile.getFileName().toString();

        try {
            ByteArrayOutputStream outputStream = zipFileToBuffer(uploadFile, entryName);

            byte[] buffer = outputStream.toByteArray();

            logger.info(format("File %s was compressed from %s to %s bytes", uploadFile, originalSize, buffer.length));

            String localMd5 = Base64.encodeBase64String(messageDigest.digest(buffer));
            return uploadToS3(bucket, key, localMd5, RequestBody.fromBytes(buffer));
        } catch (IOException e) {
            logger.info("Unable to upload (zipped) file " + uploadFile.toAbsolutePath(), e);

            return false;
        }
    }

    @NotNull
    private ByteArrayOutputStream zipFileToBuffer(Path uploadFile, String entryName) throws IOException {
        logger.info(format("Compress %s into zip, as entry %s", uploadFile, entryName));
        final FileTime lastModifiedTime = Files.getLastModifiedTime(uploadFile);

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        // entry for the file
        ZipEntry entry = new ZipEntry(entryName);
        entry.setLastModifiedTime(lastModifiedTime);

        ZipOutputStream zipOutput = new ZipOutputStream(result);

        // put entry and then the bytes for the fill
        zipOutput.putNextEntry(entry);
        byte[] bytesFromFile = Files.readAllBytes(uploadFile);
        zipOutput.write(bytesFromFile);

        zipOutput.closeEntry();
        zipOutput.close();

        result.flush();
        result.close();

        return result;
    }

    public boolean upload(String bucket, String key, String text) {
        if (!isStarted()) {
            logger.error("not started");
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(format("Uploading to bucket '%s' key '%s' contents '%s'", bucket, key, text));
        } else {
            logger.info(format("Uploading to bucket '%s' key '%s'", bucket, key));
        }

        if (!bucketExists(bucket)) {
            logger.error(format("Bucket %s does not exist", bucket));
            return false;
        }

        byte[] bytes = text.getBytes();
        String localMd5 = Base64.encodeBase64String(messageDigest.digest(bytes));
        final RequestBody requestBody = RequestBody.fromBytes(bytes);

        return uploadToS3(bucket, key, localMd5, requestBody);
    }

    private boolean uploadToS3(String bucket, String key, String localMd5, RequestBody requestBody) {
        try {
            logger.debug("Uploading with MD5: " + localMd5);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder().
                    bucket(bucket).
                    key(key).
                    build();
            s3Client.putObject(putObjectRequest, requestBody);
        } catch (AwsServiceException awsServiceException) {
            logger.error(format("AWS exception during upload for upload to bucket '%s' key '%s'", bucket, key), awsServiceException);
            return false;
        }
        return true;
    }

    public <T> List<T> downloadAndMapForKey(String bucket, String key, LiveDataClientForS3.ResponseMapper<T> responseMapper) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();

        ResponseTransformer<GetObjectResponse, List<T>> transformer =
                (response, inputStream) -> responseMapper.map(readBytes(bucket, key, response, inputStream));

        return s3Client.getObject(request, transformer);
    }

    protected byte[] readBytes(String bucket, String key, GetObjectResponse response, FilterInputStream inputStream) {
        int contentLength = Math.toIntExact(response.contentLength());

        logger.info(format("Key: %s Content type: %s Length %s ", key, response.contentType(), contentLength));
        byte[] bytes = new byte[contentLength];
        int offset = 0;
        int read;
        try {
            do {
                read = inputStream.read(bytes, offset, contentLength - offset);
                offset = offset + read;
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Key %s Read %s of %s bytes", key, read, contentLength));
                }
            } while (read > 0);
            inputStream.close();

            checkMD5(bucket, key, response, bytes);

        } catch (IOException exception) {
            logger.error("Exception downloading from bucket " + bucket + " with key " + key, exception);
        }
        return bytes;
    }

    private void checkMD5(String bucket, String key, GetObjectResponse response, byte[] bytes) {
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
            remote = remote.replaceFirst("\"", "");
        }
        if (remote.endsWith("\"")) {
            remote = remote.substring(0, remote.length() - 1);
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
        } catch (AwsServiceException exception) {
            if (exception.awsErrorDetails().errorCode().equals("NoSuchBucket")) {
                logger.info("Bucket " + bucket + " not found");
            } else {
                logger.error("Could not check for existence of bucket " + bucket, exception);
            }
            return false;
        }
        return true;
    }

    public boolean keyExists(String bucket, String prefix, String itemName) {
        if (!isStarted()) {
            logger.error("not started");
            return false;
        }

        String fullKey = prefix + "/" + itemName;

        List<S3Object> items = getSummaryForPrefix(bucket, prefix);

        for (S3Object item : items) {
            logger.debug("Check if " + item + " matches");
            if (fullKey.equals(item.key())) {
                logger.info(format("Key %s is present in bucket %s", fullKey, bucket));
                return true;
            }
        }
        logger.info(format("Key %s is not present in bucket %s", fullKey, bucket));

        return false;
    }

    public Set<String> getKeysFor(String bucket, String prefix) {
        if (!isStarted()) {
            logger.error("not started");
            return Collections.emptySet();
        }

        List<S3Object> items = getSummaryForPrefix(bucket, prefix);
        return items.stream().map(S3Object::key).collect(Collectors.toSet());
    }

    private List<S3Object> getSummaryForPrefix(String bucket, String prefix) {
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
        } catch (S3Exception exception) {
            logger.warn(format("Cannot get objects for prefix '%s' exists in bucket '%s' reason '%s'",
                    prefix, bucket, exception.getMessage()), exception);
            return Collections.emptyList();
        }

        logger.info("Found " + results.size() + " keys for bucket: " + bucket + " prefix: " + prefix);

        return results;
    }

    public boolean isStarted() {
        return s3Client != null;
    }

    public LocalDateTime getModTimeFor(String url) {
        logger.info("Fetch Mod time for url " + url);

        S3Object s3Object = getS3ObjectFor(url);

        Instant lastModified = s3Object.lastModified();
        return LocalDateTime.ofInstant(lastModified, TramchesterConfig.TimeZoneId);
    }

    private S3Object getS3ObjectFor(String url) {
        BucketKey bucketKey = BucketKey.convertFromURI(url);

        ListObjectsV2Request request = ListObjectsV2Request.builder().
                bucket(bucketKey.bucket).prefix(bucketKey.key).maxKeys(1).
                build();
        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        if (response.keyCount()!=1) {
            logger.warn(format("Unexpected number of objects, needed 1 got %s for %s", response.keyCount(), bucketKey));
        }

        if (response.contents().size()>=1) {
            return response.contents().get(0);
        } else {
            final String message = format("Unable to fetch object from %s", bucketKey);
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    private GetObjectRequest createRequestFor(BucketKey bucketKey) {
        logger.info("Create getRequest for " + bucketKey);
        return GetObjectRequest.builder().
                bucket(bucketKey.bucket).key(bucketKey.key).
                build();
    }

    public void downloadTo(Path path, String url) throws IOException {
        logger.info("Download for for url " + url);
        BucketKey bucketKey = BucketKey.convertFromURI(url);

        GetObjectRequest getObjectRequest = createRequestFor(bucketKey);
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
        String remoteMD5 = getETagClean(response.response());

        FileOutputStream output = new FileOutputStream(path.toFile());
        ByteStreams.copy(response, output);
        response.close();
        output.close();

        InputStream writtenFile = new FileInputStream(path.toFile());
        String localMd5 = DigestUtils.md5Hex(writtenFile);
        writtenFile.close();

        if (!localMd5.equals(remoteMD5)) {
            logger.warn(format("MD5 mismatch downloading from %s local '%s' remote '%s'",
                    bucketKey, localMd5, remoteMD5));
        } else  {
            logger.info(format("Downloaded to %s from %s MD5 match md5: '%s'", path.toAbsolutePath(), bucketKey, localMd5));
        }
    }



    private static class BucketKey {
        private final String bucket;
        private final String key;

        public BucketKey(String bucket, String key) {
            this.bucket = bucket;
            this.key = key;
        }

        private static BucketKey convertFromURI(String url) {
            URI uri = URI.createURI(url);
            String scheme = uri.scheme();
            if (!"s3".equals(scheme)) {
                throw new RuntimeException("s3 only, got "+scheme);
            }
            String bucket = uri.host();
            String key = uri.path().replaceFirst("/", "");
            return new BucketKey(bucket, key);
        }

        @Override
        public String toString() {
            return "BucketPrefixKey{" +
                    "bucket='" + bucket + '\'' +
                    ", key='" + key + '\'' +
                    '}';
        }
    }
}
