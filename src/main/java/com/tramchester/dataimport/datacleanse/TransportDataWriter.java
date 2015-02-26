package com.tramchester.dataimport.datacleanse;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class TransportDataWriter {
    private String path;
    private static final Logger logger = LoggerFactory.getLogger(TransportDataWriter.class);

    public TransportDataWriter(String path) {
        this.path = path;
    }

    public void writeFile(String content, String filename) throws FileNotFoundException {
        logger.info("Writing " + filename + " file...");
        PrintWriter out = new PrintWriter(path + filename + ".txt");
        out.print(content);
        out.close();
    }
}
