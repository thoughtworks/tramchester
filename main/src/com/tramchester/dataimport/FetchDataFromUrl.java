package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasRemoteDataSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
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
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class FetchDataFromUrl {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);

    private enum RefreshStatus {
        Refreshed,
        NoNeedToRefresh,
        NotExpired,
        UnableToCheck,
        Missing
    }

    // TODO Config?
    //public static final long DEFAULT_EXPIRY_MINS = 12 * 60;

    private final HttpDownloadAndModTime httpDownloader;
    private final S3DownloadAndModTime s3Downloader;
    private final List<RemoteDataSourceConfig> configs;
    private final ProvidesNow providesLocalNow;
    private final DownloadedRemotedDataRepository downloadedDataRepository;
    private final FetchFileModTime fetchFileModTime;

    @Inject
    public FetchDataFromUrl(HttpDownloadAndModTime httpDownloader, S3DownloadAndModTime s3Downloader,
                            HasRemoteDataSourceConfig config, ProvidesNow providesLocalNow,
                            DownloadedRemotedDataRepository downloadedDataRepository,
                            FetchFileModTime fetchFileModTime) {
        this.httpDownloader = httpDownloader;
        this.s3Downloader = s3Downloader;
        this.configs = config.getRemoteDataSourceConfig();
        this.providesLocalNow = providesLocalNow;

        this.downloadedDataRepository = downloadedDataRepository;
        this.fetchFileModTime = fetchFileModTime;
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
        configs.forEach(sourceConfig -> {
            final DataSourceID dataSourceId = sourceConfig.getDataSourceId();
            String targetFile = sourceConfig.getDownloadFilename();

            if (targetFile.isEmpty()) {
                String msg = format("Missing filename for %s ", dataSourceId);
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            final String prefix = "Source " + dataSourceId + ": ";
            Path downloadDirectory = sourceConfig.getDataPath();
            Path destination = downloadDirectory.resolve(targetFile);
            try {
                RefreshStatus refreshStatus = refreshDataIfNewerAvailable(sourceConfig, destination);
                logger.info(format("%s Refresh status %s", prefix, refreshStatus));
                switch (refreshStatus) {
                    case Refreshed -> {
                        downloadedDataRepository.addFileFor(dataSourceId, destination);
                        downloadedDataRepository.markRefreshed(dataSourceId);
                    }
                    case NoNeedToRefresh, NotExpired, UnableToCheck -> downloadedDataRepository.addFileFor(dataSourceId, destination);
                    case Missing -> logger.error("Unable to derive status for " + dataSourceId);
                }


            } catch (IOException | InterruptedException exception) {
                logger.warn(prefix + "Unable to refresh data for config: " + sourceConfig, exception);
            } catch (ConnectionException connectionException) {
                logger.error(prefix + "Unable to refresh data for config: " + sourceConfig, connectionException);
            }
        });
    }

    private RefreshStatus refreshDataIfNewerAvailable(RemoteDataSourceConfig sourceConfig, Path destination) throws IOException, InterruptedException {
        final DataSourceID dataSourceId = sourceConfig.getDataSourceId();

        logger.info("Refresh data if newer is available for " + dataSourceId);

        final boolean filePresent = fetchFileModTime.exists(destination);

        if (filePresent) {
            logger.info(format("Source %s file %s is present", dataSourceId, destination));
            return refreshDataIfNewerAvailableHasFile(sourceConfig, destination);
        } else {
            logger.info(format("Source %s file %s is NOT present", dataSourceId, destination));
            return refreshDataIfNewerAvailableNoFile(sourceConfig, destination);
        }

    }

    private RefreshStatus refreshDataIfNewerAvailableNoFile(RemoteDataSourceConfig sourceConfig, Path destination) throws IOException, InterruptedException {
        DataSourceID dataSourceId = sourceConfig.getDataSourceId();
        String originalURL = sourceConfig.getDataUrl();
        boolean isS3 = sourceConfig.getIsS3();

        LocalDateTime localModTime = LocalDateTime.MIN;

        // download
        URLStatus status = getUrlStatus(originalURL, isS3, localModTime, dataSourceId);
        if (status == null) {
            logger.warn(format("No local file %s and unable to check url status", destination));
            return RefreshStatus.Missing;
        }

        String actualURL = status.getActualURL();
        Path downloadDirectory = sourceConfig.getDataPath();

        logger.info(dataSourceId + ": no local file " + destination + " so down loading new data from " + actualURL);
        FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
        downloadTo(destination, actualURL, isS3, localModTime);
        return RefreshStatus.Refreshed;

    }

    private RefreshStatus refreshDataIfNewerAvailableHasFile(RemoteDataSourceConfig sourceConfig, Path existingFile) throws IOException, InterruptedException {
        // already has the source file locally
        DataSourceID dataSourceId = sourceConfig.getDataSourceId();
        boolean isS3 = sourceConfig.getIsS3();

        LocalDateTime localMod = getFileModLocalTime(existingFile);
        LocalDateTime localNow = providesLocalNow.getDateTime();

        boolean expired = localMod.plus(sourceConfig.getDefaultExpiry()).isBefore(localNow);
        logger.info(format("%s %s Local mod time: %s Current Local Time: %s ", dataSourceId, existingFile, localMod, localNow));

        // not locally expired, and no url available to check remotely for expiry
        if (sourceConfig.getDataCheckUrl().isBlank() && !expired) {
            logger.info(format("%s file: %s is not expired, skip download", dataSourceId, existingFile));
            return RefreshStatus.NoNeedToRefresh;
        }

        String originalURL = sourceConfig.getDataUrl();
        URLStatus status = getUrlStatus(originalURL, isS3, localMod, dataSourceId);
        if (status == null) return RefreshStatus.UnableToCheck;
        String actualURL = status.getActualURL();

        LocalDateTime serverMod = status.getModTime();
        if (serverMod.isEqual(LocalDateTime.MIN)) {
            logger.warn(format("%s: Unable to get mod time from server for %s", dataSourceId, actualURL));
            if (expired) {
                boolean downloaded = attemptDownload(actualURL, existingFile, isS3, localMod);
                if (downloaded) {
                    return RefreshStatus.Refreshed;
                } else {
                    logger.warn(dataSourceId + " Unable to download from " + actualURL);
                    return RefreshStatus.Missing;
                }
            } else {
                return RefreshStatus.NotExpired;
            }
        }

        logger.info(format("%s: Server mod time: %s File mod time: %s ", dataSourceId, serverMod, localMod));

        try {
            if (serverMod.isAfter(localMod)) {
                logger.warn(dataSourceId + ": server time is after local, downloading new data");
                downloadTo(existingFile, actualURL, isS3, localMod);
                return RefreshStatus.Refreshed;
            }
            logger.info(dataSourceId + ": no newer data");
            return RefreshStatus.NoNeedToRefresh;
        }
        catch (UnknownHostException disconnected) {
            logger.error(dataSourceId + " cannot connect to check or refresh data " + sourceConfig, disconnected);
            return RefreshStatus.UnableToCheck;
        }

    }

    private URLStatus getUrlStatus(String originalURL, boolean isS3, LocalDateTime localModTime, DataSourceID dataSourceId) throws IOException, InterruptedException {
        URLStatus status = getStatusFor(originalURL, isS3, localModTime);
        if (!status.isOk()) {
            if (status.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
                logger.warn("SC_METHOD_NOT_ALLOWED was unable to query using HEAD for " + dataSourceId);
            } else {
                logger.warn("Could not download for " + dataSourceId + " status was " + status);
                return null;
            }
        } else {
            logger.info(format("Got remote status %s for %s", status, dataSourceId));
        }
        return status;
    }

    private void downloadTo(Path destination, String url, boolean isS3,
                            LocalDateTime localModTime) throws IOException, InterruptedException {
        if (isS3) {
            s3Downloader.downloadTo(destination, url, localModTime);
        } else {
            httpDownloader.downloadTo(destination, url, localModTime);
        }
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

    private boolean attemptDownload(String url, Path destination, boolean isS3,
                                    LocalDateTime currentModTime)  {

        try {
            logger.info(destination + " expired downloading from " + url);
            downloadTo(destination, url, isS3, currentModTime);
            return true;
        }
        catch (IOException | InterruptedException e) {
            logger.error("Cannot download from " + url);
            return false;
        }
    }

    private LocalDateTime getFileModLocalTime(Path destination) {
        return fetchFileModTime.getFor(destination);
    }

    public static class Ready {
        private Ready() {

        }
    }

}
