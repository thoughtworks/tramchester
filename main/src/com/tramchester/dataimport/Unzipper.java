package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@LazySingleton
public class Unzipper {
    private static final Logger logger = LoggerFactory.getLogger(Unzipper.class);

    public boolean unpack(Path zipFilename, Path targetDirectory) {
        logger.info(format("Unziping data from %s to %s ", zipFilename, targetDirectory));
        File zipFile = zipFilename.toFile();
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry!=null) {
                extractEntryTo(targetDirectory, zipEntry, zipInputStream);
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();
            // update mod time
            long zipMod = zipFile.lastModified();
            logger.info("Set "+ targetDirectory.toAbsolutePath()+" mod time to : " + zipMod);
            targetDirectory.toFile().setLastModified(zipMod);

            return true;
        } catch (IOException e) {
            logger.warn("Unable to unzip "+zipFilename, e);
            return false;
        }
    }

    private void extractEntryTo(Path targetDirectory, ZipEntry zipEntry, ZipInputStream zipInputStream) throws IOException {
        Path target = targetDirectory.resolve(zipEntry.getName());

        String absolutePath = target.toAbsolutePath().toString();
        if (zipEntry.isDirectory()) {
            logger.info("Create directory " + absolutePath);
            Files.createDirectories(target);
            return;
        }

        logger.debug("Unpack file " + absolutePath);
        Path parent = target.getParent();
        if (!parent.toFile().exists()) {
            logger.info("Create needed directory " + parent + " for " +absolutePath);
            Files.createDirectories(parent);
        }

        File unpackTarget = target.toFile();
        if (unpackTarget.exists()) {
            logger.debug(absolutePath + " already exists");

            boolean modTimeMatches = checkModTime(zipEntry, unpackTarget);
            boolean sizeMatches = checkFileSize(zipEntry, unpackTarget);

            if (modTimeMatches&&sizeMatches) {
                logger.info("Not over-writing " + absolutePath);
                return;
            }
            logger.warn("Deleting " + absolutePath);
            Files.delete(target);
        }

        Files.copy(zipInputStream, target);
        unpackTarget.setLastModified(zipEntry.getLastModifiedTime().toMillis());

    }

    private boolean checkFileSize(ZipEntry zipEntry, File file) {
        if (zipEntry.getSize()==-1) {
            logger.info("No size present in zip for " + file);
            return true;
        }

        boolean sizeMatches = zipEntry.getSize() == file.length();
        if (!sizeMatches) {
            logger.warn(format("File %s exists but size (%s) does not match (%s)",
                    file, file.length(), zipEntry.getSize()));
        } else {
            logger.debug(format("File %s exists size (%s) matches (%s)",
                    file, file.length(), zipEntry.getSize()));
        }
        return sizeMatches;
    }

    private boolean checkModTime(ZipEntry zipEntry, File file) {
        long epochMilli = file.lastModified();
        boolean modTimeMatches = zipEntry.getLastModifiedTime().toMillis() == epochMilli;
        if (!modTimeMatches) {
            logger.info(format("File %s exists but mod time (%s) does not match (%s)",
                    file, file.lastModified(), zipEntry.getLastModifiedTime()));
        } else {
            logger.debug(format("File %s mod time (%s) match (%s)",
                    file, file.lastModified(), zipEntry.getLastModifiedTime()));
        }
        return modTimeMatches;
    }

}
