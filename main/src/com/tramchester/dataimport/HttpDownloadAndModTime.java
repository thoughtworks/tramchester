package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;

public class HttpDownloadAndModTime implements DownloadAndModTime {
    private static final Logger logger = LoggerFactory.getLogger(HttpDownloadAndModTime.class);

    @Override
    public LocalDateTime getModTime(String url) throws IOException {
        logger.info(format("Check mod time for %s", url));

        HttpURLConnection connection = createConnection(url);
        connection.connect();
        long serverModMillis = connection.getLastModified();

        // TODO what about redirects etc?
        boolean missing = connection.getResponseCode() != 200;
        if (missing) {
            logger.error("Response code " + connection.getResponseCode());
        }
        connection.disconnect();

        if (missing) {
            return LocalDateTime.MIN;
        }

        if (serverModMillis==0) {
            logger.warn("No valid mod time from server, got 0 for " + url);
            return LocalDateTime.MIN;
        }

        LocalDateTime modTime = getLocalDateTime(serverModMillis);
        logger.info(format("Mod time for %s is %s", url, modTime));
        return modTime;
    }

    @NotNull
    private LocalDateTime getLocalDateTime(long serverModMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(serverModMillis / 1000), TramchesterConfig.TimeZone);
    }

    @Override
    public void downloadTo(Path path, String url) throws IOException {
        try {
            File targetFile = path.toFile();
            HttpURLConnection connection = createConnection(url);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.connect();
            long len = connection.getContentLengthLong();

            long serverModMillis = connection.getLastModified();
            String contentType = connection.getContentType();
            String encoding = connection.getContentEncoding();

            logger.info("Response content type " + contentType);
            logger.info("Response encoding " + encoding);
            logger.info("Content length is " + len);

            boolean gziped = "gzip".equals(encoding);

            if (len>0) {
                downloadByLength(targetFile, connection, gziped, len);
            } else {
                download(targetFile, gziped, connection);
            }

            if (!targetFile.exists()) {
                logger.error(format("Failed to download from %s to %s", url, targetFile.getAbsoluteFile()));
            } else {
                if (serverModMillis>0) {
                    if (!targetFile.setLastModified(serverModMillis)) {
                        logger.warn("Unable to set mod time on " + targetFile);
                    }
                } else {
                    logger.warn("Server mod time is zero, not updating local file mod time");
                }
            }

            connection.disconnect();

        } catch (IOException exception) {
            logger.error(format("Unable to download data from %s to %s exception %s", url, path, exception));
            throw exception;
        }
    }

    private void download(File targetFile, boolean gziped, HttpURLConnection connection) throws IOException {
        int maxSize = 1000 * 1024 * 1024;

        final InputStream inputStream = getStreamFor(connection, gziped);
        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(targetFile);
        long received = 1;
        while (received > 0) {
            received = fos.getChannel().transferFrom(rbc, 0, maxSize);
            logger.info(format("Received %s bytes for %s", received, targetFile));
        }
        fos.close();
        rbc.close();
    }

    private void downloadByLength(File targetFile, HttpURLConnection connection, boolean gziped, long len) throws IOException {
        final InputStream inputStream = getStreamFor(connection, gziped);

        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(targetFile);
        long received = fos.getChannel().transferFrom(rbc, 0, len);
        fos.close();
        rbc.close();

        long downloadedLength = targetFile.length();
        logger.info(format("Finished download, received %s file size is %s", received, downloadedLength));
    }

    private HttpURLConnection createConnection(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }

    private InputStream getStreamFor(HttpURLConnection connection, boolean gziped) throws IOException {
        if (gziped) {
            logger.info("Response was gzip encoded, will decompress");
            return new GZIPInputStream(connection.getInputStream());
        } else {
            return connection.getInputStream();
        }
    }
}
