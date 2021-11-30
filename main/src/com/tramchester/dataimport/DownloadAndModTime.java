package com.tramchester.dataimport;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

public interface DownloadAndModTime {
    LocalDateTime getModTime(String url) throws IOException;

    void downloadTo(Path path, String url) throws IOException;
}
