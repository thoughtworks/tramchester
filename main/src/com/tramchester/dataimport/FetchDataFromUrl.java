package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasRemoteDataSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.neo4j.server.http.cypher.format.api.ConnectionException;
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
    //public static final long DEFAULT_EXPIRY_MINS = 12 * 60;

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

    // force construction via guice to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    public void fetchData() {
        configs.forEach(config -> {
            try {
                if (refreshDataIfNewerAvailable(config)) {
                    downloadedDataRepository.markRefreshed(config.getDataSourceId());
                }
            } catch (IOException | InterruptedException exception) {
                logger.warn("Unable to refresh data for config: " + config, exception);
            } catch (ConnectionException connectionException) {
                logger.error("Unable to refresh data for config: " + config, connectionException);
            }

        });
    }

    private boolean refreshDataIfNewerAvailable(RemoteDataSourceConfig config) throws IOException, InterruptedException {
        boolean isS3 = config.getIsS3();
        Path downloadDirectory = config.getDataPath();
        final DataSourceID dataSourceId = config.getDataSourceId();

        logger.info("Refresh data if newer is available for " + dataSourceId);

        String targetFile = config.getDownloadFilename();

        if (targetFile.isEmpty()) {
            logger.error(format("Missing filename for %s ", dataSourceId));
            return false;
        }

        Path destination = downloadDirectory.resolve(targetFile);

        logger.info(format("Download target for %s is %s", dataSourceId, destination));

        LocalDateTime localModTime = LocalDateTime.MIN;
        boolean expired = false;
        final boolean filePresent = Files.exists(destination);
        if (filePresent) {
            localModTime = getFileModLocalTime(destination);
            LocalDateTime localNow = providesLocalNow.getDateTime();
            expired = localModTime.plus(config.getDefaultExpiry()).isBefore(localNow);
            logger.info(format("%s Local mod time: %s Current Local Time: %s ", destination, localModTime, localNow));

            // do this here as getting the status for URL is potentially slow
            if (config.getDataCheckUrl().isBlank() && !expired) {
                logger.info(format("%s file:%s is not expired, skip download", dataSourceId, destination));
                downloadedDataRepository.addFileFor(dataSourceId, destination);
                return false;
            }
        }

        String originalURL = config.getDataUrl();
        URLStatus status = getStatusFor(originalURL, isS3, localModTime);
        if (!status.isOk()) {
            if (status.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
                logger.warn("Was unable to query using HEAD for " + config.getDataSourceId());
            } else {
                logger.error("Could not download for " + config.getDataSourceId() + " status was " + status);
                return false;
            }
        }

        String actualURL = status.getActualURL();

        if (filePresent) {
            LocalDateTime localMod = getFileModLocalTime(destination);

            LocalDateTime serverMod = status.getModTime();
            if (serverMod.isEqual(LocalDateTime.MIN)) {
                return loadIfCachePeriodExpired(expired, actualURL, destination, isS3, dataSourceId, localMod);
            }

            logger.info(format("%s: Server mod time: %s File mod time: %s ", dataSourceId, serverMod, localMod));

            try {
                if (serverMod.isAfter(localMod)) {
                    logger.warn(dataSourceId + ": server time is after local, downloading new data");
                    downloadTo(dataSourceId, destination, actualURL, isS3, localModTime);
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
            downloadTo(dataSourceId, destination, actualURL, isS3, localModTime);
            return true;
        }
    }

    private void downloadTo(DataSourceID dataSourceID, Path destination, String url, boolean isS3,
                            LocalDateTime localModTime) throws IOException, InterruptedException {
        if (isS3) {
            s3Downloader.downloadTo(destination, url, localModTime);
        } else {
            httpDownloader.downloadTo(destination, url, localModTime);
        }
        downloadedDataRepository.addFileFor(dataSourceID, destination);
    }

    private URLStatus getStatusFor(String url, boolean isS3, LocalDateTime localModTime) throws IOException, InterruptedException {
        if (isS3) {
            return s3Downloader.getStatusFor(url, localModTime);
        }
        return getStatusFollowRedirects(url, localModTime);
    }

    private URLStatus getStatusFollowRedirects(String url, LocalDateTime localModTime) throws IOException, InterruptedException {
        URLStatus status = httpDownloader.getStatusFor(url, localModTime);

        while (status.isRedirect()) {
            String redirectUrl = status.getActualURL();
            logger.warn(String.format("Status code %s Following redirect to %s", status.getStatusCode(), redirectUrl));
            status = httpDownloader.getStatusFor(redirectUrl, localModTime);
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

    private boolean loadIfCachePeriodExpired(boolean expired, String url, Path destination, boolean isS3,
                                             DataSourceID dataSourceId, LocalDateTime currentModTime)  {

        boolean downloaded = false;

        if (expired) {
            try {
                logger.info(destination + " expired downloading from " + url);
                downloadTo(dataSourceId, destination, url, isS3, currentModTime);
                downloaded = true;
            }
            catch (IOException | InterruptedException e) {
                logger.error("Cannot download from " + url);
            }
        } else {
            logger.info(destination + " not expired, using current");
        }

        return downloaded;
    }

    private LocalDateTime getFileModLocalTime(Path destination) {
        long localModMillis = destination.toFile().lastModified();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis  / 1000), TramchesterConfig.TimeZoneId);
    }

    public static class Ready {
        private Ready() {

        }
    }

}
