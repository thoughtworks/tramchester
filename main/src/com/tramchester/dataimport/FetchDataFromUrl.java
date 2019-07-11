package com.tramchester.dataimport;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

public class FetchDataFromUrl implements TransportDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);
    private Path path;
    private String dataUrl;

    public static String ZIP_FILENAME = "data.zip";

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
        Path zipFile = pullDataFromURL(ZIP_FILENAME);
        unzipData(zipFile);
    }

    public ByteArrayInputStream streamForFile(String entryWithinZip) throws IOException {
        URL website = new URL(dataUrl);

        ZipInputStream zipStream = new ZipInputStream(website.openStream());
        ZipEntry nextEntry = zipStream.getNextEntry();

        while(nextEntry!=null) {
            logger.debug("Zip stream entry " + nextEntry.getName());
            if (nextEntry.getName().equals(entryWithinZip)) {
                ByteArrayInputStream stream = createSteam(nextEntry, zipStream);
                zipStream.closeEntry();
                return stream;
            }
            zipStream.closeEntry();
            nextEntry = zipStream.getNextEntry();
        }
        zipStream.close();
        return new ByteArrayInputStream(new byte[0]);
    }

    // Warning: don't use for large files, holds whole file in memory
    private ByteArrayInputStream createSteam(ZipEntry nextEntry, ZipInputStream inputStream) throws IOException {
        Long size = nextEntry.getSize();

        byte[] buffer = new byte[size.intValue()];
        inputStream.read(buffer, 0, size.intValue());

        return new ByteArrayInputStream(buffer);
    }

    private void unzipData(Path filename) {
        logger.info("Unziping data from " + filename);
        try {
            // TODO Use native zip support in Java
            ZipFile zipFile = new ZipFile(filename.toFile());
            zipFile.extractAll(path.toAbsolutePath().toString());
        } catch (ZipException e) {
            logger.warn("Unable to unzip "+filename, e);
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
            fos.close();
        }
        catch(UnknownHostException unknownhost) {
            logger.error("Unable to download data from " + dataUrl,unknownhost);
        }

        return destination;
    }
}
