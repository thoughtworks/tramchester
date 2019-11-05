package com.tramchester.dataimport;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static java.lang.String.format;

public class Unzipper {
    private static final Logger logger = LoggerFactory.getLogger(Unzipper.class);

    public boolean unpack(Path filename, Path targetDirectory) {
        logger.info(format("Unziping data from %s to %s ", filename, targetDirectory));
        try {
            // TODO Use native zip support in Java
            ZipFile zipFile = new ZipFile(filename.toFile());
            zipFile.extractAll(targetDirectory.toAbsolutePath().toString());
            return true;
        } catch (ZipException e) {
            logger.warn("Unable to unzip "+filename, e);
            return false;
        }
    }

}
