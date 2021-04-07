package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class FetchDataFromUrlAndUnzip implements TransportDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrlAndUnzip.class);

    private final Unzipper unzipper;
    private final URLDownloadAndModTime downloader;
    private final List<GTFSSourceConfig> configs;

    @Inject
    public FetchDataFromUrlAndUnzip(Unzipper unzipper, URLDownloadAndModTime downloader, TramchesterConfig config) {
        this(unzipper, downloader, config.getGTFSDataSource());
    }

    private FetchDataFromUrlAndUnzip(Unzipper unzipper, URLDownloadAndModTime downloader, List<GTFSSourceConfig> configs) {
        this.unzipper = unzipper;
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

    // TODO Unzipper into cons
    @Override
    public void fetchData() {
        configs.forEach(config -> {
            try {
                Path zipFile = refreshDataIfNewerAvailable(config);
                if (!unzipper.unpack(zipFile, config.getDataPath())) {
                    logger.error("unable to unpack zip file " + zipFile.toAbsolutePath());
                }
            } catch (IOException e) {
                logger.info("Unable to refresh data for config " + config);
            }
        });
    }

    private void downloadAll() {
        configs.forEach(config -> {
            try {
                refreshDataIfNewerAvailable(config);
            } catch (IOException e) {
                throw new RuntimeException("Unable to refresh data for config " + config);
            }
        });
    }

    private Path refreshDataIfNewerAvailable(GTFSSourceConfig config) throws IOException {
        String url = config.getTramDataUrl();
        Path downloadDirectory = config.getDataPath();
        String targetFile = config.getZipFilename();

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
        return destination;
    }

    private LocalDateTime getFileModLocalTime(Path destination) {
        long localModMillis = destination.toFile().lastModified();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis  / 1000), TramchesterConfig.TimeZone);
    }

    // TODO Need to pass in unzip path and zip filename from CLI ags
    // used during build to download latest tram data from tfgm site during deployment
    // which is subsequently uploaded into S3
    @Deprecated
    public static void main(String[] args) throws Exception {
        if (args.length!=3) {
            throw new Exception("Expected 2 arguments, path and url");
        }
        String theUrl = args[0];
        Path folder = Paths.get(args[1]);
        String zipFilename = args[2];
        logger.info(format("Loading %s to path %s file %s", theUrl, folder, zipFilename));
        GTFSSourceConfig dataSourceConfig = new GTFSSourceConfig() {
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
                return Paths.get("./");
            }

            @Override
            public String getZipFilename() {
                return zipFilename;
            }

            @Override
            public String getName() {
                return "commandLine";
            }

            @Override
            public boolean getHasFeedInfo() {
                return true;
            }

            @Override
            public Set<GTFSTransportationType> getTransportModes() {
                return Collections.singleton(GTFSTransportationType.tram);
            }

            @Override
            public Set<TransportMode> getTransportModesWithPlatforms() {
                return Collections.singleton(TransportMode.Tram);
            }

            @Override
            public Set<LocalDate> getNoServices() {
                return Collections.emptySet();
            }
        };
        URLDownloadAndModTime downloader = new URLDownloadAndModTime();
        Unzipper unzipper = new Unzipper();
        FetchDataFromUrlAndUnzip fetcher = new FetchDataFromUrlAndUnzip(unzipper, downloader, Collections.singletonList(dataSourceConfig));
        fetcher.downloadAll();
    }

    public static class Ready {
        private Ready() {

        }
    }

}
