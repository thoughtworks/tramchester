package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.ClientForS3;

import javax.inject.Inject;
import java.io.IOException;
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
    public LocalDateTime getModTime(String url) throws IOException {
        return s3Client.getModTimeFor(url);
    }

    @Override
    public void downloadTo(Path path, String url) throws IOException {
        s3Client.downloadTo(path, url);
    }
}
