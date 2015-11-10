package com.tramchester.dataimport;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;

public class FetchDataFromUrl implements TransportDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);
    private String path;
    private String dataUrl;

    public FetchDataFromUrl(String path, String dataUrl) {
        this.path = path;
        this.dataUrl = dataUrl;
    }

    @Override
    public void fetchData() throws IOException {
        String filename = "data.zip";
        Path zipFile = pullDataFromURL(filename, new URL(dataUrl));
        unzipData(zipFile);
    }

    private void unzipData(Path filename) {
        logger.info("Unziping data from " + filename);
        try {
            ZipFile zipFile = new ZipFile(filename.toFile());
            zipFile.extractAll(path);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    private Path pullDataFromURL(String targetFile, URL website) throws IOException {
        Path destination = Paths.get(path, targetFile);
        logger.info(format("Downloading data from %s to %s", website, destination));

        FileUtils.forceMkdir(new File(path));
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(destination.toFile());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        logger.info("Finished download");
        return destination;
    }
}
