package com.tramchester.dataimport.datacleanse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class TransportDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataWriterFactory.class);

    private final PrintWriter out;

    public TransportDataWriter(Path path, String filename) throws IOException {
        if (!path.toFile().exists()) {
            logger.info("Create directory " + path.toString());
            Files.createDirectory(path);
        }
        Path fullPath = path.resolve(filename + ".txt");
        logger.info("Create writer for " + fullPath.toAbsolutePath());
        out = new PrintWriter(fullPath.toFile());
    }

    public void writeLine(String line) {
        out.println(line);
    }

    public void close() {
        out.flush();
        out.close();
    }
}
