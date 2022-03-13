package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasRemoteDataSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
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
public class FetchDataFromUrl {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);

    // TODO Config?
    public static final long DEFAULT_EXPIRY_MINS = 12 * 60;

    private final HttpDownloadAndModTime httpDownloader;
    private final S3DownloadAndModTime s3Downloader;
    private final List<RemoteDataSourceConfig> configs;
    private final ProvidesNow providesLocalNow;
    private final DownloadedRemotedDataRepository downloadedDataRepository;

    @Inject
    public FetchDataFromUrl(HttpDownloadAndModTime httpDownloader, S3DownloadAndModTime s3Downloader,
                            HasRemoteDataSourceConfig config, ProvidesNow providesLocalNow,
                            DownloadedRemotedDataRepository downloadedDataRepository) {
        this.httpDownloader = httpDownloader;
        this.s3Downloader = s3Downloader;
        this.configs = config.getRemoteDataSourceConfig();
        this.providesLocalNow = providesLocalNow;

        this.downloadedDataRepository = downloadedDataRepository;
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
                    downloadedDataRepository.markRefreshed(config.getDataSourceId());
                }
            } catch (IOException e) {
                logger.info("Unable to refresh data for config " + config);
            }
        });
    }

    private boolean refreshDataIfNewerAvailable(RemoteDataSourceConfig config) throws IOException {
        boolean isS3 = config.getIsS3();
        Path downloadDirectory = config.getDataPath();
        final DataSourceID dataSourceId = config.getDataSourceId();

        logger.info("Refresh data if newer is available for " + dataSourceId);

        String targetFile;
        URLStatus status = null;

        if (config.getDownloadFilename().isEmpty()) {
            status = getStatusFromRemoteURL(config, isS3);
            if (!status.isOk()) {
                return false;
            }
            targetFile = status.getFilename();
        } else {
            targetFile = config.getDownloadFilename();
        }

        if (targetFile.isEmpty()) {
            logger.error(format("Unable to calculate download target for %s from '%s' or '%s'",
                    dataSourceId, config.getDownloadFilename(), config.getDataUrl()));
            return false;
        }

        Path destination = downloadDirectory.resolve(targetFile);

        logger.info(format("Download target for %s is %s", dataSourceId, destination));

        boolean expired = false;
        final boolean filePresent = Files.exists(destination);
        if (filePresent) {
            LocalDateTime localMod = getFileModLocalTime(destination);
            LocalDateTime localNow = providesLocalNow.getDateTime();
            expired = localMod.plusMinutes(DEFAULT_EXPIRY_MINS).isBefore(localNow);
            logger.info(format("%s Local mod time: %s Current Local Time: %s ", destination, localMod, localNow));

            // do this here as getting the status for URL is potentially slow
            if (config.getDataCheckUrl().isBlank() && !expired) {
                logger.info(format("%s file:%s is not expired, skip download", dataSourceId, destination));
                downloadedDataRepository.addFileFor(dataSourceId, destination);
                return false;
            }
        }

        if (status==null) {
            // don't do this twice, is potentially expensive
            String originalURL = config.getDataUrl();
            status = getStatusFor(originalURL, isS3);
            if (!status.isOk()) {
                return false;
            }
        }

        String actualURL = status.getActualURL();

        if (filePresent) {
            LocalDateTime localMod = getFileModLocalTime(destination);

            LocalDateTime serverMod = status.getModTime();
            if (serverMod.isEqual(LocalDateTime.MIN)) {
                return loadIfCachePeriodExpired(expired, actualURL, destination, isS3, dataSourceId);
            }

            logger.info(format("%s: Server mod time: %s File mod time: %s ", dataSourceId, serverMod, localMod));

            try {
                if (serverMod.isAfter(localMod)) {
                    logger.warn(dataSourceId + ": server time is after local, downloading new data");
                    downloadTo(dataSourceId, destination, actualURL, isS3);
                    return true;
                }
                logger.info(dataSourceId + ": no newer data");
                downloadedDataRepository.addFileFor(dataSourceId, destination);
                return false;
            }
            catch (UnknownHostException disconnected) {
                logger.error("Cannot connect to check or refresh data " + config, disconnected);
                return false;
            }
        } else {
            logger.info(dataSourceId + ": no local file " + destination + " so down loading new data from " + actualURL);
            FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
            downloadTo(dataSourceId, destination, actualURL, isS3);
            return true;
        }
    }

    private URLStatus getStatusFromRemoteURL(RemoteDataSourceConfig config, boolean isS3) throws IOException {
        URLStatus status;
        String originalURL = config.getDataUrl();

        logger.info("target file is not provided, derive from download url: " + originalURL);

        status = getStatusFor(originalURL, isS3);

        if (!status.isOk()) {
            logger.error(format("Unable to get target filename, since status %s for Requested URL %s ", status.getStatusCode(), originalURL));
        }
        return status;
    }

    private void downloadTo(DataSourceID dataSourceID, Path destination, String url, boolean isS3) throws IOException {
        if (isS3) {
            s3Downloader.downloadTo(destination, url);
        } else {
            httpDownloader.downloadTo(destination, url);
        }
        downloadedDataRepository.addFileFor(dataSourceID, destination);
    }

    private URLStatus getStatusFor(String url, boolean isS3) throws IOException {
        if (isS3) {
            return s3Downloader.getStatusFor(url);
        }
        return getStatusFollowRedirects(url);
    }

    private URLStatus getStatusFollowRedirects(String url) throws IOException {
        URLStatus status = httpDownloader.getStatusFor(url);

        while (status.isRedirect()) {
            String redirectUrl = status.getActualURL();
            logger.warn(String.format("Status code %s Following redirect to %s", status.getStatusCode(), redirectUrl));
            status = httpDownloader.getStatusFor(redirectUrl);
        }

        String message = String.format("Status: %s final url: '%s'",
                status.getStatusCode(), status.getActualURL());

        if (status.isOk()) {
            logger.info(message);
        } else {
            logger.error(message);
        }

        return status;
    }

    private boolean loadIfCachePeriodExpired(boolean expired, String url, Path destination, boolean isS3, DataSourceID dataSourceId)  {

        boolean downloaded = false;

        if (expired) {
            try {
                logger.info(destination + " expired downloading from " + url);
                downloadTo(dataSourceId, destination, url, isS3);
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

    public static class Ready {
        private Ready() {

        }
    }

}
