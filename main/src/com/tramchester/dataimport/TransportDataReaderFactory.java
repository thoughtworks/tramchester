package com.tramchester.dataimport;

import com.tramchester.config.DownloadConfig;

import java.nio.file.Path;

public class TransportDataReaderFactory {
    private final DownloadConfig config;
    private TransportDataReader readerForCleanser;
    private TransportDataReader readerForLoader;

    public TransportDataReaderFactory(DownloadConfig config) {
        this.config = config;
    }

    public TransportDataReader getForCleanser() {
        if (readerForCleanser==null) {
            Path path = config.getDataPath().resolve(config.getUnzipPath());
            readerForCleanser = new TransportDataReader(path, true);
        }
        return readerForCleanser;
    }

    public TransportDataReader getForLoader() {
        if (readerForLoader==null) {
            readerForLoader = new TransportDataReader(config.getDataPath(), false);
        }
        return readerForLoader;
    }
}
