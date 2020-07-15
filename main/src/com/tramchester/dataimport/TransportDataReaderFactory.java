package com.tramchester.dataimport;

import com.tramchester.config.DownloadConfig;

import java.nio.file.Path;

public class TransportDataReaderFactory implements TransportDataLoader {
    private final DownloadConfig config;
    private TransportDataReader readerForCleanser;
    private TransportDataReader readerForLoader;

    public TransportDataReaderFactory(DownloadConfig config) {
        this.config = config;
    }

    public TransportDataReader getForCleanser() {
        if (readerForCleanser==null) {
            Path path = config.getDataPath().resolve(config.getUnzipPath());
            DataLoaderFactory factory = new DataLoaderFactory(path, ".txt");
            readerForCleanser = new TransportDataReader(factory, true);
        }
        return readerForCleanser;
    }

    public TransportDataReader getForLoader() {
        if (readerForLoader==null) {
            DataLoaderFactory factory = new DataLoaderFactory(config.getDataPath(), ".txt");
            readerForLoader = new TransportDataReader(factory,  false);
        }
        return readerForLoader;
    }
}
