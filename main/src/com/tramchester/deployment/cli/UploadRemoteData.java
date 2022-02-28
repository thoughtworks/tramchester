package com.tramchester.deployment.cli;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;

@LazySingleton
public class UploadRemoteData {
    private static final Logger logger = LoggerFactory.getLogger(UploadRemoteData.class);

    private final UploadFileToS3 uploadFileToS3;
    private final TramchesterConfig config;

    @Inject
    public UploadRemoteData(UploadFileToS3 uploadFileToS3, TramchesterConfig config) {
        this.uploadFileToS3 = uploadFileToS3;
        this.config = config;
    }

    public boolean upload() {
        List<RemoteDataSourceConfig> remoteSources = config.getRemoteDataSourceConfig();
        return remoteSources.stream().allMatch(this::upload);
    }

    private boolean upload(RemoteDataSourceConfig remoteSource) {
        final Path path = remoteSource.getDataPath();
        final String downloadFilename = remoteSource.getDownloadFilename();
        logger.info("Update data source " + remoteSource.getName() + " for path " + path + " and file " + downloadFilename);

        String prefix = path.toString();
        Path fullFilename = path.resolve(downloadFilename);

        return uploadFileToS3.uploadFile(prefix, fullFilename);
    }
}
