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

    public boolean uploadFile(String prefix, Path fileToUpload) {
        if (!clientForS3.isStarted()) {
            throw new RuntimeException("S3 client is not started");
        }

        String filename = fileToUpload.getFileName().toString();
        if (clientForS3.keyExists(bucket, prefix, filename)) {
            logger.error(format("prefix %s key %s already exists", prefix, filename));
            return false;
        }

        final String key = prefix + "/" + filename;
        logger.info(format("Update file %s to bucket %s at %s", fileToUpload, bucket, filename));
        return clientForS3.upload(bucket, key, fileToUpload);
    }

}
