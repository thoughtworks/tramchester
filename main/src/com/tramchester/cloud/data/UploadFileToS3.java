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
     * @param prefixForKey where to place the item
     * @param fileToUpload the item to upload
     * @param overWrite over-write the item if already present
     * @return true if file uploads ok, false otherwise
     */
    public boolean uploadFile(String prefixForKey, Path fileToUpload, boolean overWrite) {
        guardStarted();

        logger.info(format("Upload %s to %s overwrite:%s", fileToUpload, prefixForKey, overWrite));

        String itemId = fileToUpload.getFileName().toString();

        if (clientForS3.keyExists(bucket, prefixForKey, itemId)) {
            final String overwriteMessage = format("prefix %s key %s already exists", prefixForKey, itemId);
            if (overWrite) {
                logger.warn(overwriteMessage);
            } else {
                logger.error(overwriteMessage);
                return false;
            }
        }

        final String key = prefixForKey + "/" + itemId;
        logger.info(format("Upload file %s to bucket %s at %s", fileToUpload.toAbsolutePath(), bucket, key));
        return clientForS3.upload(bucket, key, fileToUpload);
    }

    public boolean uploadFileZipped(String prefixForKey, Path fileToUpdate, boolean overwrite) {
        guardStarted();

        logger.info(format("Upload zipped %s to %s overwrite:%s", fileToUpdate, prefixForKey, overwrite));

        String itemId = fileToUpdate.getFileName().toString() + ".zip";

        if (clientForS3.keyExists(bucket, prefixForKey, itemId)) {
            final String overwriteMessage = format("prefix %s key %s already exists", prefixForKey, itemId);
            if (overwrite) {
                logger.warn(overwriteMessage);
            } else {
                logger.error(overwriteMessage);
                return false;
            }
        }

        final String key = prefixForKey + "/" + itemId;
        logger.info(format("Upload file %s zipped to bucket %s at %s", fileToUpdate.toAbsolutePath(), bucket, key));

        return clientForS3.uploadZipped(bucket, key, fileToUpdate);
    }

    private void guardStarted() {
        if (!clientForS3.isStarted()) {
            throw new RuntimeException("S3 client is not started");
        }
    }


}
