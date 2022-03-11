package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.ClientForS3;
import org.apache.http.HttpStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

@LazySingleton
public class S3DownloadAndModTime implements DownloadAndModTime {

    private final ClientForS3 s3Client;

    @Inject
    public S3DownloadAndModTime(ClientForS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public URLStatus getStatusFor(String url) {
        return new URLStatus(url, HttpStatus.SC_OK, s3Client.getModTimeFor(url));
    }

    @Override
    public void downloadTo(Path path, String url) throws IOException {
        s3Client.downloadTo(path, url);
    }
}
