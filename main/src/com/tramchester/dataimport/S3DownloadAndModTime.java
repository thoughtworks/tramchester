package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.ClientForS3;
import org.apache.http.HttpStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;

@LazySingleton
public class S3DownloadAndModTime implements DownloadAndModTime {

    private final ClientForS3 s3Client;

    @Inject
    public S3DownloadAndModTime(ClientForS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public URLStatus getStatusFor(URI url, LocalDateTime localModTime) {
        return new URLStatus(url, HttpStatus.SC_OK, s3Client.getModTimeFor(url));
    }

    @Override
    public URLStatus downloadTo(Path path, URI url, LocalDateTime localModTime) throws IOException {
        // there is no equivalent to if-modified-since so don't use localModTime here
        return s3Client.downloadTo(path, url);
    }

}
