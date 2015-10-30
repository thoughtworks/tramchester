package com.tramchester.dataimport.datacleanse;


import java.io.FileNotFoundException;

public class TransportDataWriterFactory {
    private String path;

    public TransportDataWriterFactory(String path) {
        this.path = path;
    }

    public TransportDataWriter getWriter(String filename) throws FileNotFoundException {
        return new TransportDataWriter(path,filename);
    }
}
