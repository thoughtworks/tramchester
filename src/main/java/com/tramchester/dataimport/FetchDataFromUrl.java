package com.tramchester.dataimport;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

public class FetchDataFromUrl implements TransportDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);
    private Path path;
    private String dataUrl;

    public FetchDataFromUrl(Path path, String dataUrl) {
        this.path = path;
        this.dataUrl = dataUrl;
    }

    // used during build to download latest tram data from tfgm site
    public static void main(String[] args) throws Exception {
        if (args.length!=3) {
            throw new Exception("Expected 2 arguments, path and url");
        }
        String theUrl = args[0];
        Path thePath = Paths.get(args[1]);
        String theFile = args[2];
        logger.info(format("Loading %s to path %s file %s", theUrl, thePath, theFile));
        FetchDataFromUrl fetcher = new FetchDataFromUrl(thePath, theUrl);
        fetcher.pullDataFromURL(theFile);
    }

    @Override
    public void fetchData() throws IOException {
        String filename = "data.zip";
        Path zipFile = pullDataFromURL(filename);
        unzipData(zipFile);
    }

    private void unzipData(Path filename) {
        logger.info("Unziping data from " + filename);
        try {
            ZipFile zipFile = new ZipFile(filename.toFile());
            zipFile.extractAll(path.toAbsolutePath().toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    private Path pullDataFromURL(String targetFile) throws IOException {
        Path destination = path.resolve(targetFile);
        URL website = new URL(dataUrl);
        logger.info(format("Downloading data from %s to %s", website, destination));

        FileUtils.forceMkdir(path.toFile());
        try {
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(destination.toFile());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            logger.info("Finished download");
        }
        catch(UnknownHostException unknownhost) {
            logger.error("Unable to download data from " + dataUrl,unknownhost);
        }

        return destination;
    }
}
