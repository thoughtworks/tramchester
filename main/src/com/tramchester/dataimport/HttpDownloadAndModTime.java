package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import org.apache.http.HttpStatus;
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
    public URLStatus getStatusFor(String originalUrl) throws IOException {
        logger.debug(format("Check status %s", originalUrl));

        HttpURLConnection connection = createConnection(originalUrl);
        connection.connect();
        long serverModMillis = connection.getLastModified();
        int httpStatusCode = connection.getResponseCode();

        String finalUrl = originalUrl;
        final boolean redirect = httpStatusCode == HttpStatus.SC_MOVED_PERMANENTLY || httpStatusCode == HttpStatus.SC_MOVED_TEMPORARILY;
        if (redirect) {
            String locationField = connection.getHeaderField("Location");
            if (!locationField.isBlank()) {
                logger.warn(format("Redirect status %s and Location header '%s'", httpStatusCode, locationField));
                finalUrl = locationField;
            } else {
                logger.error(format("Location header missing for redirect %s, change status code to a 404 for %s",
                        httpStatusCode, originalUrl));
                httpStatusCode = HttpStatus.SC_NOT_FOUND;
            }
        }

        connection.disconnect();

        URLStatus result;
        if (serverModMillis==0) {
            if (!redirect) {
                logger.warn("No valid mod time from server, got 0 for " + originalUrl);
            }
            result = new URLStatus(finalUrl, httpStatusCode);
        } else {
            LocalDateTime modTime = getLocalDateTime(serverModMillis);
            logger.debug(format("Mod time for %s is %s", originalUrl, modTime));
            result = new URLStatus(finalUrl, httpStatusCode, modTime);
        }

        if (!result.isOk() && !result.isRedirect()) {
            logger.warn("Response code " + httpStatusCode + " for " + originalUrl);
        }

        logger.info(result.toString());

        return result;
    }

    @NotNull
    private LocalDateTime getLocalDateTime(long serverModMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(serverModMillis / 1000), TramchesterConfig.TimeZone);
    }

    @Override
    public void downloadTo(Path path, String url) throws IOException {
        try {
            logger.info(format("Download from %s to %s", url, path.toAbsolutePath()));
            File targetFile = path.toFile();
            HttpURLConnection connection = createConnection(url);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.connect();
            long len = connection.getContentLengthLong();

            long serverModMillis = connection.getLastModified();
            String contentType = connection.getContentType();
            String encoding = connection.getContentEncoding();

            final String suffix = " for " + url;
            logger.info("Response content type " + contentType + suffix);
            logger.info("Response encoding " + encoding + suffix);
            logger.info("Content length is " + len + suffix);

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
                    logger.warn("Server mod time is zero, not updating local file mod time " + suffix);
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

        long downloadedLength = targetFile.length();
        logger.info(format("Finished download, file %s size is %s", targetFile.getPath(), downloadedLength));
    }

    private void downloadByLength(File targetFile, HttpURLConnection connection, boolean gziped, long len) throws IOException {
        final InputStream inputStream = getStreamFor(connection, gziped);

        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(targetFile);
        long received = fos.getChannel().transferFrom(rbc, 0, len);
        fos.close();
        rbc.close();

        long downloadedLength = targetFile.length();
        logger.info(format("Finished download, received %s, file %s size is %s",
                received, targetFile.getPath(), downloadedLength));
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
