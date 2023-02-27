package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.DateUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;

public class HttpDownloadAndModTime implements DownloadAndModTime {
    private static final Logger logger = LoggerFactory.getLogger(HttpDownloadAndModTime.class);

    @Override
    public URLStatus getStatusFor(String originalUrl, LocalDateTime localModTime) throws IOException, InterruptedException {

        // TODO some servers return 200 for HEAD but a redirect status for a GET
        // So cannot rely on using the HEAD request for getting final URL for a resource
        HttpResponse<Void> response = fetchHeaders(URI.create(originalUrl), localModTime, HttpMethod.HEAD,
                HttpResponse.BodyHandlers.discarding());
        HttpHeaders headers = response.headers();

        long serverModMillis = getServerModMillis(response);

        int httpStatusCode = response.statusCode();

        final boolean redirect = URLStatus.isRedirectCode(httpStatusCode);

        // might update depending on redirect status
        String finalUrl = originalUrl;
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

        return createURLStatus(finalUrl, serverModMillis, httpStatusCode, redirect);
    }

    private long getServerModMillis(HttpResponse<?> response) {
        HttpHeaders headers = response.headers();
        Optional<String> lastModifiedHeader = headers.firstValue(org.apache.http.HttpHeaders.LAST_MODIFIED);

        long serverModMillis = 0;
        if (lastModifiedHeader.isPresent()) {
            final String lastMod = lastModifiedHeader.get();
            logger.info("Mod time for " + response.uri() + " was " + lastMod + " status " + response.statusCode());
            Date modTime = DateUtils.parseDate(lastMod);
            serverModMillis = modTime.getTime();
        } else {
            logger.warn("No mod time header for " + response.uri() + " status " + response.statusCode());
            logger.info("Headers were: ");
            headers.map().forEach((head, contents) -> logger.info(head + ": " + contents));
        }
        return serverModMillis;
    }

    private <T> HttpResponse<T> fetchHeaders(URI uri, LocalDateTime localLastMod, String method,
                                             HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().
                uri(uri).
                method(method, HttpRequest.BodyPublishers.noBody());

        if (localLastMod != LocalDateTime.MIN) {
            ZonedDateTime httpLocalModTime = localLastMod.atZone(ZoneId.of("Etc/UTC"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtils.PATTERN_RFC1036);
            final String headerIfModSince = formatter.format(httpLocalModTime);
            logger.info(format("Checking uri with %s : %s", HttpHeader.IF_MODIFIED_SINCE.name(), headerIfModSince));
            httpRequestBuilder.header(HttpHeader.IF_MODIFIED_SINCE.name(), headerIfModSince);
        }

        // setRequestProperty("Accept-Encoding", "gzip");
        if (HttpMethod.GET.equals(method)) {
            httpRequestBuilder.header(HttpHeader.ACCEPT_ENCODING.name(), "gzip");
        }

        HttpRequest httpRequest = httpRequestBuilder.build();

        return client.send(httpRequest, bodyHandler);
    }

    @NotNull
    private URLStatus createURLStatus(String url, long serverModMillis, int httpStatusCode, boolean redirected) {
        URLStatus result;
        if (serverModMillis == 0) {
            if (!redirected) {
                logger.warn(format("No valid mod time from server, got 0, status code %s for %s", httpStatusCode, url));
            }
            result = new URLStatus(url, httpStatusCode);

        } else {
            LocalDateTime modTime = getLocalDateTime(serverModMillis);
            logger.debug(format("Mod time %s, status %s for %s", modTime, url, httpStatusCode));
            result = new URLStatus(url, httpStatusCode, modTime);
        }

        if (!result.isOk()) { // && !result.isRedirect()) {
            logger.warn("Response code " + httpStatusCode + " for " + url);
        }

        return result;
    }

    @NotNull
    private LocalDateTime getLocalDateTime(long serverModMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(serverModMillis / 1000), TramchesterConfig.TimeZoneId);
    }

    @Override
    public URLStatus downloadTo(Path path, String url, LocalDateTime existingLocalModTime) throws IOException, InterruptedException {
        try {
            logger.info(format("Download from %s to %s", url, path.toAbsolutePath()));
            File targetFile = path.toFile();

            HttpResponse<InputStream> response = fetchHeaders(URI.create(url), existingLocalModTime, HttpMethod.GET,
                    HttpResponse.BodyHandlers.ofInputStream());

            int statusCode = response.statusCode();
            if (statusCode != HttpStatus.SC_OK) {
                logger.warn("Status code on download not OK, got " + statusCode);
                return new URLStatus(url, statusCode);
            }

            long serverModMillis = getServerModMillis(response);
            String contentType = getContentType(response);
            String encoding = getContentEncoding(response);
            long len = getLen(response);

            String contentDispos = getContentDispos(response);
            if (!contentDispos.isEmpty()) {
                logger.warn("Content disposition was " + contentDispos);
            }

            final String logSuffix = " for " + url;
            final LocalDateTime serverModDateTime = getLocalDateTime(serverModMillis);
            logger.info("Response last mod time is " + serverModMillis + " (" + serverModDateTime + ")");
            logger.info("Response content type '" + contentType + "'" + logSuffix);
            logger.info("Response encoding '" + encoding + "'" + logSuffix);
            logger.info("Content length is " + len + logSuffix);

            boolean gziped = "gzip".equals(encoding);

            InputStream stream = getStreamFor(response.body(), gziped);
            if (len>0) {
                downloadByLength(stream, targetFile, len);
            } else {
                download(stream, targetFile);
            }

            if (!targetFile.exists()) {
                String msg = format("Failed to download from %s to %s", url, targetFile.getAbsoluteFile());
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            URLStatus result;
            if (serverModMillis>0) {
                result = new URLStatus(url, statusCode, getLocalDateTime(serverModMillis));
                if (!targetFile.setLastModified(serverModMillis)) {
                    logger.warn("Unable to set mod time on " + targetFile);
                } else {
                    logger.info("Set mod time on " + targetFile + " to " + serverModDateTime);
                }
            } else {
                result = new URLStatus(url, statusCode);
                logger.warn("Server mod time is zero, not updating local file mod time " + logSuffix);
            }

            return result;


        } catch (IOException | InterruptedException exception) {
            String msg = format("Unable to download data from %s to %s exception %s", url, path, exception);
            logger.error(msg);
            throw new RuntimeException(msg,exception);
        }

    }

    private void download(InputStream inputStream, File targetFile) throws IOException {
        int maxSize = 1000 * 1024 * 1024;

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

    private void downloadByLength(InputStream inputStream, File targetFile, long len) throws IOException {

        ReadableByteChannel rbc = Channels.newChannel(inputStream);
        FileOutputStream fos = new FileOutputStream(targetFile);
        long received = fos.getChannel().transferFrom(rbc, 0, len);
        fos.close();
        rbc.close();

        long downloadedLength = targetFile.length();
        logger.info(format("Finished download, received %s, file %s size is %s",
                received, targetFile.getPath(), downloadedLength));
    }

    private InputStream getStreamFor(InputStream inputStream, boolean gziped) throws IOException {
        if (gziped) {
            logger.info("Response was gzip encoded, will decompress");
            return new GZIPInputStream(inputStream);
        } else {
            return inputStream;
        }
    }

    private long getLen(HttpResponse<InputStream> response) {
        OptionalLong header = response.headers().firstValueAsLong(HttpHeader.CONTENT_LENGTH.name());
        return header.orElse(0);
    }

    private String getContentEncoding(HttpResponse<?> response) {
        Optional<String> contentEncoding = response.headers().firstValue(HttpHeader.CONTENT_TYPE.name());
        return contentEncoding.orElse("");
    }

    private String getContentType(HttpResponse<?> response) {
        Optional<String> contentTypeHeader = response.headers().firstValue(HttpHeader.CONTENT_TYPE.name());
        return contentTypeHeader.orElse("");
    }

    private String getContentDispos(HttpResponse<?> response) {
        Optional<String> header = response.headers().firstValue("content-disposition");
        if (header.isPresent()) {
            return header.get();
        }
        header = response.headers().firstValue("Content-Disposition");
        return header.orElse("");
    }
}
