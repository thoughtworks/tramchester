package com.tramchester.dataimport;

import java.io.IOException;
import java.nio.file.Path;

public interface DownloadAndModTime {
    HttpDownloadAndModTime.URLStatus getModTime(String url) throws IOException;

    void downloadTo(Path path, String url) throws IOException;
}
