package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;

public class HttpDownloadAndModTime implements DownloadAndModTime {
    private static final Logger logger = LoggerFactory.getLogger(HttpDownloadAndModTime.class);

    @Override
    public URLStatus getStatusFor(String originalUrl) throws IOException, InterruptedException {

//        HttpURLConnection connection = createConnection(originalUrl);
//        connection.connect();

        HttpResponse<Void> response = getHeadersFor(URI.create(originalUrl));
        HttpHeaders headers = response.headers();

        Optional<String> lastModifiedHeader = headers.firstValue(org.apache.http.HttpHeaders.LAST_MODIFIED);

        long serverModMillis = 0;
        if (lastModifiedHeader.isPresent()) {
            final String lastMod = lastModifiedHeader.get();
            logger.info("Mod time for " + originalUrl + " was " + lastMod);
            Date modTime = DateUtils.parseDate(lastMod);
            serverModMillis = modTime.getTime();
        } else {
            logger.warn("No mod time header for " + originalUrl);
        }

        int httpStatusCode = response.statusCode();

        String finalUrl = originalUrl;
        final boolean redirect = httpStatusCode == HttpStatus.SC_MOVED_PERMANENTLY
                || httpStatusCode == HttpStatus.SC_MOVED_TEMPORARILY;

        if (redirect) {
            Optional<String> locationField = headers.firstValue(org.apache.http.HttpHeaders.LOCATION); // connection.getHeaderField("Location");
            if (locationField.isPresent()) {
                logger.warn(format("URL: '%s' Redirect status %s and Location header '%s'",
                        originalUrl, httpStatusCode, locationField));
                finalUrl = locationField.get();
            } else {
                logger.error(format("Location header missing for redirect %s, change status code to a 404 for %s",
                        httpStatusCode, originalUrl));
                httpStatusCode = HttpStatus.SC_NOT_FOUND;
            }
        } else {
            if (httpStatusCode!=HttpStatus.SC_OK) {
                logger.warn("Got error status code "+httpStatusCode+ " headers follow");
                headers.map().forEach((header, values) -> logger.info("Header: " + header + " Value: " +values));
            }
        }

//        String filename = "";
//        Optional<String> contentDisposHeader = headers.firstValue("Content-Disposition"); //connection.getHeaderField("content-disposition");
//        if (contentDisposHeader.isEmpty()) {
//            contentDisposHeader = headers.firstValue("content-disposition");
//        }
//        if (contentDisposHeader.isPresent()) {
//            String contentDispos = contentDisposHeader.get();
//            filename = getFilenameFromHeader(contentDispos);
//            logger.info(format("Got filename '%s' from Content-Disposition header: '%s'", filename, contentDispos));
//        }

        //connection.disconnect();

        return createURLStatus(finalUrl, serverModMillis, httpStatusCode, redirect);
    }

    private HttpResponse<Void> getHeadersFor(URI uri) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();

        HttpResponse<Void> response = getHttpResponse(uri, client, HttpMethod.HEAD);


//        if (response.statusCode()==HttpStatus.SC_METHOD_NOT_ALLOWED) {
//            logger.warn("Method not allowed for HEAD at " + uri + " so fall back to GET");
//            response = getHttpResponse(uri, client, HttpMethod.GET);
//            logger.info("Finished GET");
//        }

        return response;
    }

    private HttpResponse<Void> getHttpResponse(URI uri, HttpClient client, String method) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder().
                uri(uri).
                method(method, HttpRequest.BodyPublishers.noBody()).
                build();

        return client.send(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    @NotNull
    private URLStatus createURLStatus(String url, long serverModMillis, int httpStatusCode, boolean redirected) {
        URLStatus result;
        if (serverModMillis == 0) {
            if (!redirected) {
                logger.warn("No valid mod time from server, got 0 for " + url);
            }
            result = new URLStatus(url, httpStatusCode);

        } else {
            LocalDateTime modTime = getLocalDateTime(serverModMillis);
            logger.debug(format("Mod time for %s is %s", url, modTime));
            result = new URLStatus(url, httpStatusCode, modTime);
        }

//        if (!filename.isBlank()) {
//            result.setFilename(filename);
//        }

        if (!result.isOk() && !result.isRedirect()) {
            logger.warn("Response code " + httpStatusCode + " for " + url);
        }

        return result;
    }

    @NotNull
    private LocalDateTime getLocalDateTime(long serverModMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(serverModMillis / 1000), TramchesterConfig.TimeZoneId);
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

            String actualRemoteName = getContentDispos(connection);
            if (!actualRemoteName.isEmpty()) {
                logger.warn("Remote filename was " + actualRemoteName);
            }

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
                    } else {
                        logger.info("Set mod time on " + targetFile + " to " + serverModMillis + " (" +
                                getLocalDateTime(serverModMillis) + ")");
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

    private String getContentDispos(HttpURLConnection connection) {
        String disposition = connection.getHeaderField("content-disposition");
        if (disposition !=null) {
            return disposition;
        }
        disposition = connection.getHeaderField("Content-Disposition");
        if (disposition !=null) {
            return disposition;
        }
        return "";
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

//    private String getFilenameFromHeader(String header) {
//        try {
//            ContentDisposition contentDisposition = new ContentDisposition(header);
//            return contentDisposition.getFileName();
//        } catch (ParseException e) {
//            logger.warn(format("Unable to parse content-disposition '%s'", header));
//            return "";
//        }
//
//    }

}
