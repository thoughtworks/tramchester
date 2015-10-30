package com.tramchester.dataimport.datacleanse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class TransportDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataWriterFactory.class);

    private final PrintWriter out;

    public TransportDataWriter(String path, String filename) throws FileNotFoundException {
        logger.info("Create writer for " + path + "/" + filename);
        out = new PrintWriter(path + filename + ".txt");
    }

    public void writeLine(String line) {
        out.println(line);
    }

    public void close() {
        out.close();
    }
}
