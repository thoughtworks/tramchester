package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@LazySingleton
public class Unzipper {
    private static final Logger logger = LoggerFactory.getLogger(Unzipper.class);

    public boolean unpack(Path zipFilename, Path targetDirectory) {
        logger.info(format("Unziping data from %s to %s ", zipFilename, targetDirectory));
        try {
            File zipFile = zipFilename.toFile();
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
        Path name = targetDirectory.resolve(zipEntry.getName());

        String absolutePath = name.toAbsolutePath().toString();
        if (zipEntry.isDirectory()) {
            logger.info("Create directory " + absolutePath);
            Files.createDirectories(name);
            return;
        }
        logger.debug("Unpack file " + absolutePath);
        Path parent = name.getParent();
        if (!parent.toFile().exists()) {
            logger.info("Create needed directory " + parent + " for " +absolutePath);
            Files.createDirectories(parent);
        }
        File unpackTarget = name.toFile();
        if (unpackTarget.exists()) {
            logger.debug(absolutePath + " already exists");
            long epochMilli = unpackTarget.lastModified();
            boolean modTime = zipEntry.getLastModifiedTime().toMillis() == epochMilli;
            boolean size = zipEntry.getSize() == unpackTarget.length();
            if (!modTime) {
                logger.warn("File exists but mod time does not match");
            }
            if (!size) {
                logger.warn("File exists but size does not match");
            }
            if (modTime&&size) {
                logger.debug("Not over-writing " + absolutePath);
                return;
            }
            logger.warn("Deleting " + absolutePath);
            Files.delete(name);
        }
        Files.copy(zipInputStream, name);
        unpackTarget.setLastModified(zipEntry.getLastModifiedTime().toMillis());

    }

}
