package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;

import static java.lang.String.format;

public class URLDownloadAndModTime {
    private static final Logger logger = LoggerFactory.getLogger(URLDownloadAndModTime.class);

    public LocalDateTime getModTime(String url) throws IOException {
        logger.info(format("Check mod time for %s", url));

        HttpURLConnection connection = createConnection(url);
        connection.connect();
        long serverModMillis = connection.getLastModified();
        connection.disconnect();

        LocalDateTime modTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(serverModMillis / 1000), TramchesterConfig.TimeZone);

        logger.info(format("Mod time for %s is %s", url, modTime));
        return modTime;
    }

    public void downloadTo(Path path, String url) throws IOException {
        try {
            File targetFile = path.toFile();
            HttpURLConnection connection = createConnection(url);
            connection.connect();
            long len = connection.getContentLengthLong();
            long serverModMillis = connection.getLastModified();

            logger.info("Content length is " + len);

            if (len>0) {
                ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
                FileOutputStream fos = new FileOutputStream(targetFile);
                fos.getChannel().transferFrom(rbc, 0, len);
                logger.info("Finished download");
                fos.close();
                rbc.close();

                if (!targetFile.setLastModified(serverModMillis)) {
                    logger.warn("Unable to set mod time on " + targetFile);
                }
            } else {
                logger.error("Unable to download from " + url);
            }

            connection.disconnect();

        } catch (IOException exception) {
            logger.error(format("Unable to download data from %s to %s exception %s", url, path, exception));
            throw exception;
        }
    }

    private HttpURLConnection createConnection(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }
}
