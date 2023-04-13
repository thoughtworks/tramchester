package com.tramchester.dataimport;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;

public interface DownloadAndModTime {
    URLStatus getStatusFor(URI uri, LocalDateTime localModTime) throws IOException, InterruptedException;

    URLStatus downloadTo(Path path, URI uri, LocalDateTime localModTime) throws IOException, InterruptedException;

}
