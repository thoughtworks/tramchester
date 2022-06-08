package com.tramchester.dataimport;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

public interface DownloadAndModTime {
    URLStatus getStatusFor(String url, LocalDateTime localModTime) throws IOException, InterruptedException;

    void downloadTo(Path path, String url, LocalDateTime localModTime) throws IOException, InterruptedException;
}
