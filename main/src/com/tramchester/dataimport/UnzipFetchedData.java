package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;

@LazySingleton
public class UnzipFetchedData  {
    private static final Logger logger = LoggerFactory.getLogger(UnzipFetchedData.class);

    private final Unzipper unzipper;
    private final List<RemoteDataSourceConfig> configs;
    private final RemoteDataAvailable remoteDataAvailable;

    @Inject
    public UnzipFetchedData(Unzipper unzipper, TramchesterConfig config, RemoteDataAvailable remoteDataRefreshed,
                            FetchDataFromUrl.Ready ready) {
        this(unzipper, config.getRemoteDataSourceConfig(), remoteDataRefreshed, ready);
    }

    private UnzipFetchedData(Unzipper unzipper, List<RemoteDataSourceConfig> configs,
                             RemoteDataAvailable remoteDataAvailable, FetchDataFromUrl.Ready ready) {
        this.unzipper = unzipper;
        this.configs = configs;
        this.remoteDataAvailable = remoteDataAvailable;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        unzipAsNeeded();
        logger.info("started");
    }

    // force contsruction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    private void unzipAsNeeded() {
        if (configs.isEmpty()) {
            logger.info("No configs present");
        }

        configs.forEach(sourceConfig -> {
            final DataSourceID sourceId = sourceConfig.getDataSourceId();

            if (remoteDataAvailable.hasFileFor(sourceId)) {
                Path filename = remoteDataAvailable.fileFor(sourceId);
                if (!unzipper.unpackIfZipped(filename, sourceConfig.getDataPath())) {
                    String msg = "unable to unpack zip file " + filename.toAbsolutePath();
                    logger.error(msg);
                    throw new RuntimeException(msg); // fail fast
                }
            } else {
                String msg = "No file available for " + sourceId + " config was " + sourceConfig;
                logger.error(msg);
                throw new RuntimeException(msg); // fail fast
            }
        });
    }

    public static class Ready {
        private Ready() {

        }
        public static Ready fakeForTestingOnly() {
            return new Ready();
        }
    }

}
