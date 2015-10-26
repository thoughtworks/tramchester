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

public class FetchDataFromUrl {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);
    private String path;

    public FetchDataFromUrl(String path) {
        this.path = path;
    }

    public void fetchData(String dataUrl) throws IOException {
        String filename = "data.zip";
        pullDataFromURL(filename, new URL(dataUrl));
        unzipData(filename);
    }

    private void unzipData(String filename) {
        logger.info("Unziping data...");
        try {
            ZipFile zipFile = new ZipFile(path + filename);
            zipFile.extractAll(path);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    private void pullDataFromURL(String filename, URL website) throws IOException {
        logger.info("Downloading data from " + website);

        FileUtils.forceMkdir(new File(path));
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(path + filename);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        logger.info("Finished download");
    }
}
