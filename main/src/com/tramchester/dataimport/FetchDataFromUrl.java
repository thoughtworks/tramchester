package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class FetchDataFromUrl implements TransportDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);

    private final URLDownloadAndModTime downloader;
    private final List<RemoteDataSourceConfig> configs;

    @Inject
    public FetchDataFromUrl(URLDownloadAndModTime downloader, TramchesterConfig config) {
        this(downloader, config.getRemoteDataSourceConfig());
    }

    private FetchDataFromUrl(URLDownloadAndModTime downloader, List<RemoteDataSourceConfig> configs) {
        this.downloader = downloader;
        this.configs = configs;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        fetchData();
        logger.info("started");
    }

    // force contsruction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Override
    public void fetchData() {
        configs.forEach(config -> {
            try {
                refreshDataIfNewerAvailable(config);
            } catch (IOException e) {
                logger.info("Unable to refresh data for config " + config);
            }
        });
    }

    private void refreshDataIfNewerAvailable(RemoteDataSourceConfig config) throws IOException {
        String url = config.getDataUrl();
        Path downloadDirectory = config.getDataPath();
        String targetFile = config.getDownloadFilename();

        Path destination = downloadDirectory.resolve(targetFile);

        String name = config.getName();
        if (Files.exists(destination)) {
            try {
                // check mod times
                LocalDateTime serverMod = downloader.getModTime(url);
                LocalDateTime localMod = getFileModLocalTime(destination);
                logger.info(format("%s: Server mod time: %s File mod time: %s ", name, serverMod, localMod));

                if (serverMod.isAfter(localMod)) {
                    logger.warn(name + ": server time is after local, downloading new data");
                    downloader.downloadTo(destination, url);
                } else if (serverMod.equals(LocalDateTime.MIN)) {
                    logger.error("Missing source: " + url);
                } else {
                    logger.info(name + ": no newer data");
                }
            }
            catch (UnknownHostException disconnected) {
                logger.error("Cannot connect to check or refresh data " + config, disconnected);
            }
        } else {
            logger.info(name + ": no local file " + destination + " so down loading new data");
            FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
            downloader.downloadTo(destination, url);
        }
    }

    private LocalDateTime getFileModLocalTime(Path destination) {
        long localModMillis = destination.toFile().lastModified();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis  / 1000), TramchesterConfig.TimeZone);
    }

    public static class Ready {
        private Ready() {

        }
    }

}
