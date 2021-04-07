package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
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
public class FetchDataFromUrl  {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);

    // TODO Config?
    public static final long DEFAULT_EXPIRY_MINS = 12 * 60;

    private final URLDownloadAndModTime downloader;
    private final List<RemoteDataSourceConfig> configs;
    private final ProvidesNow providesLocalNow;

    @Inject
    public FetchDataFromUrl(URLDownloadAndModTime downloader, TramchesterConfig config, ProvidesNow providesLocalNow) {
        this(downloader, config.getRemoteDataSourceConfig(), providesLocalNow);
    }

    public FetchDataFromUrl(URLDownloadAndModTime downloader, List<RemoteDataSourceConfig> configs, ProvidesNow providesLocalNow) {
        this.downloader = downloader;
        this.configs = configs;
        this.providesLocalNow = providesLocalNow;
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
            LocalDateTime localMod = getFileModLocalTime(destination);

            if (config.getDataCheckUrl().isBlank()) {
                // skip mod time check
                logger.info("Skipping mod time check for " + config.getName());
                loadIfCachePeriodExpired(url, destination, localMod);
                return;
            }

            LocalDateTime serverMod = downloader.getModTime(url);
            if (serverMod.isEqual(LocalDateTime.MIN)) {
                loadIfCachePeriodExpired(url, destination, localMod);
                return;
            }

            if (serverMod.isEqual(LocalDateTime.MAX)) {
                logger.error("Requested URL is (not status 200) missing " + url);
                return;
            }

            logger.info(format("%s: Server mod time: %s File mod time: %s ", name, serverMod, localMod));

            try {
                if (serverMod.isAfter(localMod)) {
                    logger.warn(name + ": server time is after local, downloading new data");
                    downloader.downloadTo(destination, url);
                } else {
                    logger.info(name + ": no newer data");
                }
            }
            catch (UnknownHostException disconnected) {
                logger.error("Cannot connect to check or refresh data " + config, disconnected);
            }
        } else {
            logger.info(name + ": no local file " + destination + " so down loading new data from " + url);
            FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
            downloader.downloadTo(destination, url);
        }
    }

    private void loadIfCachePeriodExpired(String url, Path destination, LocalDateTime fileModTime)  {
        LocalDateTime localNow = providesLocalNow.getDateTime();
        String prefix = format("Local mod time: %s Current Local Time: %s ", fileModTime, localNow);
        try {
            if (fileModTime.plusMinutes(DEFAULT_EXPIRY_MINS).isBefore(localNow)) {
                logger.info(prefix + " expired downloading from " + url);
                downloader.downloadTo(destination, url);
            } else {
                logger.info(prefix + " not expired, using current " + destination);
            }
        } catch (IOException e) {
            logger.error("Cannot download from " + url);
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
