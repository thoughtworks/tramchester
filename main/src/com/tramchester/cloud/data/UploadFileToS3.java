package com.tramchester.cloud.data;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;

import static java.lang.String.format;

@LazySingleton
public class UploadFileToS3 {
    private static final Logger logger = LoggerFactory.getLogger(UploadFileToS3.class);

    private final ClientForS3 clientForS3;
    private final String bucket;

    @Inject
    public UploadFileToS3(ClientForS3 clientForS3, TramchesterConfig config) {
        this.clientForS3 = clientForS3;
        this.bucket = config.getDistributionBucket();
    }

    /***
     * Upload a file to S3, DOES NOT overwrite an existing item
     * @param s3Key where to place the item
     * @param fileToUpload the item to upload
     * @return true if file uploads ok, false otherwise
     */
    public boolean uploadFile(String s3Key, Path fileToUpload) {
        if (!clientForS3.isStarted()) {
            throw new RuntimeException("S3 client is not started");
        }

        String itemId = fileToUpload.getFileName().toString();

        if (clientForS3.keyExists(bucket, s3Key, itemId)) {
            logger.warn(format("prefix %s key %s already exists", s3Key, itemId));
            return false;
        }

        final String key = s3Key + "/" + itemId;
        logger.info(format("Upload file %s to bucket %s at %s", fileToUpload.toAbsolutePath(), bucket, key));
        return clientForS3.upload(bucket, key, fileToUpload);
    }

}
