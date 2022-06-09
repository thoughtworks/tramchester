package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@LazySingleton
public class Unzipper {
    private static final Logger logger = LoggerFactory.getLogger(Unzipper.class);

    PathMatcher zipMatcher = FileSystems.getDefault().getPathMatcher("glob:**.zip");

    public boolean unpackIfZipped(Path filename, Path targetDirectory) {
        File zipFile = filename.toFile();
        try {
            if (zipMatcher.matches(filename)) {
                int entries = 0;
                logger.info(format("Unziping data from %s to %s ", filename, targetDirectory));
                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {
                    entries++;
                    extractEntryTo(targetDirectory, zipEntry, zipInputStream);
                    zipInputStream.closeEntry();
                    zipEntry = zipInputStream.getNextEntry();
                }
                zipInputStream.close();
                if (entries==0) {
                    logger.warn("Unzipped zero entries, was this a zip file? " + filename);
                }
            } else {
                logger.info(format("Skipping unzip, %s not a zip file", zipFile.getAbsoluteFile()));
            }
            // update mod time
            updateFileModTime(targetDirectory, zipFile);

            return true;
        } catch (ZipException zipException) {
            logger.error("Unable to unzip, zip exception ", zipException);
            return false;
        }
        catch (FileNotFoundException fileNotFoundException) {
            logger.error("File is missing ", fileNotFoundException);
            return false;
        } catch (IOException ioException) {
            logger.error("IOException while processing zip file ", ioException);
            return false;
        }

    }

    private void updateFileModTime(Path targetDirectory, File zipFile) {
        long zipMod = zipFile.lastModified();
        logger.info("Set '" + targetDirectory.toAbsolutePath() + "' mod time to: " + zipMod);
        boolean undatedModTime = targetDirectory.toFile().setLastModified(zipMod);
        if (!undatedModTime) {
            logger.warn("Could not update the modification time of " + targetDirectory.toAbsolutePath());
        }
    }

    private void extractEntryTo(Path targetDirectory, ZipEntry zipEntry, ZipInputStream zipInputStream) throws IOException {
        Path target = targetDirectory.resolve(zipEntry.getName());
        logger.info("Extracting entry " + toLogString(zipEntry));

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

            if (modTimeMatches && sizeMatches) {
                logger.info("Not over-writing " + absolutePath);
                return;
            }
            logger.warn("Deleting " + absolutePath);
            Files.delete(target);
        }

        try {
            Files.copy(zipInputStream, target);
            boolean setModTime = unpackTarget.setLastModified(zipEntry.getLastModifiedTime().toMillis());
            if (!setModTime) {
                logger.warn("Could not set mod time on " + absolutePath);
            }
        } catch (IOException e) {
            logger.error("Exception while extracting entry :'" + toLogString(zipEntry) + "' to '" + absolutePath + "'");
        }


    }

    // toString on zipEntry is just the name../.
    private String toLogString(ZipEntry zipEntry) {
        return String.format("zipEntry{name:%s size:%s comp size: %s method:%s}", zipEntry.getName(), zipEntry.getSize(),
                zipEntry.getCompressedSize(), zipEntry.getMethod());
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
