package com.tramchester.dataimport;

import com.tramchester.config.DownloadConfig;
import com.tramchester.config.TramchesterConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;

import static java.lang.String.format;

public class FetchDataFromUrl implements TransportDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);

    private Path downloadDirectory;
    private URLDownloader downloader;

    public static String ZIP_FILENAME = "data.zip";
    private String url;

    public FetchDataFromUrl(URLDownloader downloader, DownloadConfig config) {
        this.downloader = downloader;
        this.downloadDirectory = config.getDataPath();
        url = config.getTramDataUrl();
    }

    // used during build to download latest tram data from tfgm site during deployment
    // which is subsequently uploaded into S3
    public static void main(String[] args) throws Exception {
        if (args.length!=3) {
            throw new Exception("Expected 2 arguments, path and url");
        }
        String theUrl = args[0];
        Path folder = Paths.get(args[1]);
        String zipFilename = args[2];
        logger.info(format("Loading %s to path %s file %s", theUrl, folder, zipFilename));
        DownloadConfig downloadConfig = new DownloadConfig() {
            @Override
            public String getTramDataUrl() {
                return theUrl;
            }

            @Override
            public String getTramDataCheckUrl() {
                return null;
            }

            @Override
            public Path getDataPath() {
                return folder;
            }

            @Override
            public Path getUnzipPath() {
                return Paths.get("gtdf-out");
            }
        };
        URLDownloader downloader = new URLDownloader();
        FetchDataFromUrl fetcher = new FetchDataFromUrl(downloader, downloadConfig);
        fetcher.refreshDataIfNewerAvailable(zipFilename);
    }

    @Override
    public void fetchData(Unzipper unzipper) throws IOException {
        Path zipFile = refreshDataIfNewerAvailable(ZIP_FILENAME);
        if (!unzipper.unpack(zipFile, downloadDirectory)) {
            logger.error("unable to unpack zip file " + zipFile.toAbsolutePath());
        }
    }

    private Path refreshDataIfNewerAvailable(String targetFile) throws IOException {
        Path destination = this.downloadDirectory.resolve(targetFile);

        if (Files.exists(destination)) {
            try {
                // check mod times
                LocalDateTime serverMod = downloader.getModTime(url);
                LocalDateTime localMod = getFileModLocalTime(destination);
                logger.info(format("Server mod time: %s File mod time: %s ", serverMod, localMod));

                if (serverMod.isAfter(localMod)) {
                    logger.warn("Server time is after local, downing new data");
                    downloader.downloadTo(destination, url);
                } else {
                    logger.info("No newer data");
                }
            }
            catch (UnknownHostException disconnected) {
                logger.error("Cannot connect to check or refresh data", disconnected);
            }
        } else {
            logger.info("No local file " + destination + " so down loading new data");
            FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
            downloader.downloadTo(destination, url);
        }
        return destination;
    }

    private LocalDateTime getFileModLocalTime(Path destination) {
        long localModMillis = destination.toFile().lastModified();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis  / 1000), TramchesterConfig.TimeZone);
    }

}
