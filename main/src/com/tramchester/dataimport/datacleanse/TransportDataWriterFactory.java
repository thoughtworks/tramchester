package com.tramchester.dataimport.datacleanse;


import com.tramchester.config.DownloadConfig;

import java.io.IOException;
import java.nio.file.Path;

public class TransportDataWriterFactory {
    private Path path;

    public TransportDataWriterFactory(DownloadConfig config) {
        this.path = config.getDataPath();
    }

    public TransportDataWriter getWriter(String filename) throws IOException {
        return new TransportDataWriter(path,filename);
    }
}
