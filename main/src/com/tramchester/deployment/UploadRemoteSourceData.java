package com.tramchester.deployment;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class UploadRemoteSourceData {
    private static final Logger logger = LoggerFactory.getLogger(UploadRemoteSourceData.class);

    private final UploadFileToS3 uploadFileToS3;
    private final TramchesterConfig config;

    @Inject
    public UploadRemoteSourceData(UploadFileToS3 uploadFileToS3, TramchesterConfig config) {
        this.uploadFileToS3 = uploadFileToS3;
        this.config = config;
    }

    public boolean upload(String prefixForS3Key) {
        List<RemoteDataSourceConfig> remoteSources = config.getRemoteDataSourceConfig();
        return remoteSources.stream().allMatch(dataSource -> upload(prefixForS3Key, dataSource));
    }

    private boolean upload(String prefixForS3Key, RemoteDataSourceConfig remoteSource) {
        final Path path = remoteSource.getDataPath();
        final String downloadFilename = remoteSource.getDownloadFilename();
        logger.info(format("Upload data source %s for %s %s and key %s'",
                remoteSource.getName(), path, downloadFilename, prefixForS3Key));

        Path fullFilename = path.resolve(downloadFilename);

        boolean result = uploadFileToS3.uploadFile(prefixForS3Key, fullFilename);
        if (!result) {
            logger.error("Unable to upload for " + remoteSource.getName() + " check above logs for failures");
        }
        return result;
    }
}
