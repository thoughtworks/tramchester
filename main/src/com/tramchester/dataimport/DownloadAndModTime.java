package com.tramchester.dataimport;

import java.io.IOException;
import java.nio.file.Path;

public interface DownloadAndModTime {
    URLStatus getStatusFor(String url) throws IOException, InterruptedException;

    void downloadTo(Path path, String url) throws IOException;
}
