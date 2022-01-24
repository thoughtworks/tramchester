package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasRemoteDataSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
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
        String originalURL = config.getDataUrl();
        Path downloadDirectory = config.getDataPath();
        String targetFile = config.getDownloadFilename();
        String dataSourceName = config.getName();

        Path destination = downloadDirectory.resolve(targetFile);

        boolean expired = false;
        final boolean filePresent = Files.exists(destination);
        if (filePresent) {
            LocalDateTime localMod = getFileModLocalTime(destination);
            LocalDateTime localNow = providesLocalNow.getDateTime();
            expired = localMod.plusMinutes(DEFAULT_EXPIRY_MINS).isBefore(localNow);
            logger.info(format("%s Local mod time: %s Current Local Time: %s ", destination, localMod, localNow));

            // do this here as getting the status for URL is potentially slow
            if (config.getDataCheckUrl().isBlank() && !expired) {
                logger.info(format("%s file:%s is not expired, skip download", dataSourceName, destination));
                return false;
            }
        }

        HttpDownloadAndModTime.URLStatus status = getStatusFor(originalURL, isS3);
        if (!status.isOk()) {
            logger.error(format("Status %s for Requested URL %s ", status.getStatusCode(), originalURL));
            return false;
        }
        String actualURL = status.getActualURL();

        if (filePresent) {
            LocalDateTime localMod = getFileModLocalTime(destination);

//            if (config.getDataCheckUrl().isBlank()) {
//                // skip mod time check
//                logger.info("Skipping mod time check for " + config.getName());
//                return loadIfCachePeriodExpired(expired, actualURL, destination, isS3);
//            }

            LocalDateTime serverMod = status.getModTime();
            if (serverMod.isEqual(LocalDateTime.MIN)) {
                return loadIfCachePeriodExpired(expired, actualURL, destination, isS3);
            }

            logger.info(format("%s: Server mod time: %s File mod time: %s ", dataSourceName, serverMod, localMod));

            try {
                if (serverMod.isAfter(localMod)) {
                    logger.warn(dataSourceName + ": server time is after local, downloading new data");
                    downloadTo(actualURL, destination, isS3);
                    return true;
                }
                logger.info(dataSourceName + ": no newer data");
                return false;
            }
            catch (UnknownHostException disconnected) {
                logger.error("Cannot connect to check or refresh data " + config, disconnected);
                return false;
            }
        } else {
            logger.info(dataSourceName + ": no local file " + destination + " so down loading new data from " + actualURL);
            FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
            downloadTo(actualURL, destination, isS3);
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

    private HttpDownloadAndModTime.URLStatus getStatusFor(String url, boolean isS3) throws IOException {
        if (isS3) {
            return s3Downloader.getStatusFor(url);
        }
        return getStatusFollowRedirects(url);
    }

    private HttpDownloadAndModTime.URLStatus getStatusFollowRedirects(String url) throws IOException {
        HttpDownloadAndModTime.URLStatus status = httpDownloader.getStatusFor(url);

        while (status.isRedirect()) {
            String redirectUrl = status.getActualURL();
            String message = String.format("Status code %s Following redirect to %s", status.getStatusCode(), redirectUrl);
            if (status.getStatusCode()==HttpStatus.SC_MOVED_TEMPORARILY) {
                logger.warn(message);
            } else {
                logger.error(message);
            }
            status = httpDownloader.getStatusFor(redirectUrl);
        }

        return status;
    }

    private boolean loadIfCachePeriodExpired(boolean expired, String url, Path destination, boolean isS3)  {

        boolean downloaded = false;

        if (expired) {
            try {
                logger.info(destination + " expired downloading from " + url);
                downloadTo(url, destination, isS3);
                downloaded = true;
            }
            catch (IOException e) {
                logger.error("Cannot download from " + url);
            }
        } else {
            logger.info(destination + " not expired, using current");
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
