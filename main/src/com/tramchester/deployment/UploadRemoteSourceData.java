package com.tramchester.deployment;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.domain.DataSourceID;
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
    private final RemoteDataRefreshed remoteDataRefreshed;

    @Inject
    public UploadRemoteSourceData(UploadFileToS3 uploadFileToS3, TramchesterConfig config, RemoteDataRefreshed remoteDataRefreshed) {
        this.uploadFileToS3 = uploadFileToS3;
        this.config = config;
        this.remoteDataRefreshed = remoteDataRefreshed;
    }

    public boolean upload(String prefixForS3Key) {
        List<RemoteDataSourceConfig> remoteSources = config.getRemoteDataSourceConfig();
        return remoteSources.stream().
                filter(dataSource -> remoteDataRefreshed.hasFileFor(dataSource.getDataSourceId())).
                allMatch(dataSource -> upload(prefixForS3Key, dataSource.getDataSourceId()));
    }

    private boolean upload(String prefixForS3Key, DataSourceID dataSourceId) {

        final Path path = remoteDataRefreshed.fileFor(dataSourceId);

        logger.info(format("Upload data source %s for %s and key %s'", dataSourceId, path, prefixForS3Key));

        String filename = path.getFileName().toString();

        boolean result;
        if (filename.toLowerCase().endsWith(".xml")) {
            result = uploadFileToS3.uploadFileZipped(prefixForS3Key, path, true);
        } else {
            result = uploadFileToS3.uploadFile(prefixForS3Key, path, true);
        }

        if (!result) {
            logger.error("Unable to upload for " + dataSourceId + " check above logs for failures");
        }
        return result;
    }
}
