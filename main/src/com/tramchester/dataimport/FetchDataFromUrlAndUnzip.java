package com.tramchester.dataimport;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static java.lang.String.format;

public class FetchDataFromUrlAndUnzip {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrlAndUnzip.class);

    // used during build to download latest tram data from tfgm site during deployment
    // which is subsequently uploaded into S3
    public static void main(String[] args) throws Exception {
        if (args.length!=3) {
            throw new RuntimeException("Expected 3 arguments: URL, taget folder, target filename");
        }
        String theUrl = args[0];
        Path folder = Paths.get(args[1]);
        String downloadFilename = args[2];
        logger.info(format("Loading from %s to path %s file %s", theUrl, folder, downloadFilename));

        RemoteDataSourceConfig dataSourceConfig = createConfig(theUrl, folder, downloadFilename);

        URLDownloadAndModTime downloader = new URLDownloadAndModTime();

        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();
        FetchDataFromUrl fetchDataFromUrl = new FetchDataFromUrl(downloader,
                Collections.singletonList(dataSourceConfig), providesLocalNow);
        fetchDataFromUrl.start();
    }

    @NotNull
    private static RemoteDataSourceConfig createConfig(String theUrl, Path folder, String downloadFilename) {
        return new RemoteDataSourceConfig() {
            @Override
            public String getDataCheckUrl() {
                return theUrl;
            }

            @Override
            public String getDataUrl() {
                return theUrl;
            }

            @Override
            public String getDownloadFilename() {
                return downloadFilename;
            }

            @Override
            public String getName() {
                return "commandline";
            }

            @Override
            public Path getDataPath() {
                return folder;
            }
        };
    }
}
