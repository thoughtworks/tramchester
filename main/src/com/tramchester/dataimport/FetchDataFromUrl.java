package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasRemoteDataSourceConfig;
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
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class FetchDataFromUrl implements RemoteDataRefreshed {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);

    // TODO Config?
    public static final long DEFAULT_EXPIRY_MINS = 12 * 60;

    // TODO create facade to choose correct downloader based on url schem
    private final HttpDownloadAndModTime httpDownloader;
    private final S3DownloadAndModTime s3Downloader;

    private final List<RemoteDataSourceConfig> configs;
    private final ProvidesNow providesLocalNow;
    private final List<String> refreshed;

    @Inject
    public FetchDataFromUrl(HttpDownloadAndModTime httpDownloader, S3DownloadAndModTime s3Downloader,
                            HasRemoteDataSourceConfig config, ProvidesNow providesLocalNow) {
        this.httpDownloader = httpDownloader;
        this.s3Downloader = s3Downloader;
        this.configs = config.getRemoteDataSourceConfig();
        this.providesLocalNow = providesLocalNow;
        refreshed = new ArrayList<>();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (this.configs==null) {
            throw new RuntimeException("configs should be null, use empty list if not sources needed");
        }
        fetchData();
        logger.info("started");
    }

    // force construction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    public void fetchData() {
        configs.forEach(config -> {
            try {
                if (refreshDataIfNewerAvailable(config)) {
                    refreshed.add(config.getName());
                }
            } catch (IOException e) {
                logger.info("Unable to refresh data for config " + config);
            }
        });
    }

    private boolean refreshDataIfNewerAvailable(RemoteDataSourceConfig config) throws IOException {
        boolean isS3 = config.getIsS3();
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
                return loadIfCachePeriodExpired(url, destination, localMod, isS3);
            }

            LocalDateTime serverMod = getModTime(url, isS3);
            if (serverMod.isEqual(LocalDateTime.MIN)) {
                return loadIfCachePeriodExpired(url, destination, localMod, isS3);
            }

            if (serverMod.isEqual(LocalDateTime.MAX)) {
                logger.error("Requested URL is (not status 200) missing " + url);
                return false;
            }

            logger.info(format("%s: Server mod time: %s File mod time: %s ", name, serverMod, localMod));

            try {
                if (serverMod.isAfter(localMod)) {
                    logger.warn(name + ": server time is after local, downloading new data");
                    downloadTo(url, destination, isS3);
                    return true;
                }
                logger.info(name + ": no newer data");
                return false;
            }
            catch (UnknownHostException disconnected) {
                logger.error("Cannot connect to check or refresh data " + config, disconnected);
                return false;
            }
        } else {
            logger.info(name + ": no local file " + destination + " so down loading new data from " + url);
            FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
            downloadTo(url, destination, isS3);
            return true;
        }
    }

    private void downloadTo(String url, Path destination, boolean isS3) throws IOException {
        if (isS3) {
            s3Downloader.downloadTo(destination, url);
        } else {
            httpDownloader.downloadTo(destination, url);
        }
    }

    private LocalDateTime getModTime(String url, boolean isS3) throws IOException {
        if (isS3) {
            return s3Downloader.getModTime(url);
        }
        return httpDownloader.getModTime(url);
    }

    private boolean loadIfCachePeriodExpired(String url, Path destination, LocalDateTime fileModTime, boolean isS3)  {
        LocalDateTime localNow = providesLocalNow.getDateTime();
        String prefix = format("Local mod time: %s Current Local Time: %s ", fileModTime, localNow);
        boolean downloaded = false;
        try {
            if (fileModTime.plusMinutes(DEFAULT_EXPIRY_MINS).isBefore(localNow)) {
                logger.info(prefix + " expired downloading from " + url);
                downloadTo(url, destination, isS3);
                downloaded = true;
            } else {
                logger.info(prefix + " not expired, using current " + destination);
            }
        } catch (IOException e) {
            logger.error("Cannot download from " + url);
        }
        return downloaded;
    }

    private LocalDateTime getFileModLocalTime(Path destination) {
        long localModMillis = destination.toFile().lastModified();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis  / 1000), TramchesterConfig.TimeZone);
    }

    @Override
    public boolean refreshed(String name) {
        return refreshed.contains(name);
    }

    public static class Ready {
        private Ready() {

        }
    }

}
