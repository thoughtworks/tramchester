package com.tramchester.dataimport.datacleanse;


import java.io.IOException;
import java.nio.file.Path;

public class TransportDataWriterFactory {
    private Path path;

    public TransportDataWriterFactory(Path path) {
        this.path = path;
    }

    public TransportDataWriter getWriter(String filename) throws IOException {
        return new TransportDataWriter(path,filename);
    }
}
