package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class UnzipFetchedData  {
    private static final Logger logger = LoggerFactory.getLogger(UnzipFetchedData.class);

    private final Unzipper unzipper;
    private final List<RemoteDataSourceConfig> configs;

    @Inject
    public UnzipFetchedData(Unzipper unzipper, TramchesterConfig config, FetchDataFromUrl.Ready ready) {
        this(unzipper, config.getRemoteDataSourceConfig());
    }

    private UnzipFetchedData(Unzipper unzipper, List<RemoteDataSourceConfig> configs) {
        this.unzipper = unzipper;
        this.configs = configs;
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
        configs.forEach(config -> {
            Path zipFile = config.getDataPath().resolve(config.getDownloadFilename());
            if (!unzipper.unpack(zipFile, config.getDataPath())) {
                logger.error("unable to unpack zip file " + zipFile.toAbsolutePath());
            }
        });
    }

    public static class Ready {
        private Ready() {

        }
    }

}
