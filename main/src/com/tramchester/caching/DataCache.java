package com.tramchester.caching;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.DataSaver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@LazySingleton
public class DataCache {
    private static final Logger logger = LoggerFactory.getLogger(DataCache.class);

    private final Path cacheFolder;
    private boolean ready;

    @Inject
    public DataCache(TramchesterConfig config) {
        this.cacheFolder = config.getCacheFolder().toAbsolutePath();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        ready = false;
        if (Files.exists(cacheFolder)) {
            logger.info("Cached folder exists at " + cacheFolder);
            ready = true;
        } else {
            try {
                logger.info("Created folder at " + cacheFolder);
                Files.createDirectories(cacheFolder);
                ready = true;
            } catch (IOException exception) {
                logger.warn("Could not create cache folder ", exception);
            }
        }
    }

    @PreDestroy
    public void stop() {
        ready = false;
        try {
            List<Path> filesInCacheDir = filesInCache();
            filesInCacheDir.forEach(file -> logger.info("File present " + file));
        } catch (IOException e) {
            logger.error("Could not list files in " + cacheFolder);
        }
    }

    @NotNull
    private List<Path> filesInCache() throws IOException {
        return Files.list(cacheFolder).filter(Files::isRegularFile).collect(Collectors.toList());
    }

    public <T> void save(Cacheable<T> data, Class<T> theClass) {
        final Path path = getPathFor(data);

        if (ready) {
            logger.info("Saving " + theClass.getSimpleName() + " to " + path);
            DataSaver<T> saver = new DataSaver<>(theClass, path);
            data.cacheTo(saver);
        } else {
            logger.error("Not ready, no data saved to " + path);
        }
    }

    public <T> boolean has(Cacheable<T> cacheable) {
        return Files.exists(getPathFor(cacheable));
    }

    @NotNull
    private <T> Path getPathFor(Cacheable<T> data) {
        String filename = data.getFilename();
        return cacheFolder.resolve(filename).toAbsolutePath();
    }

    public void clearFiles() {
        try {
            List<Path> files = filesInCache();
            for (Path file : files) {
                Files.delete(file);
            }
        } catch (IOException exception) {
            final String msg = "Unable to clear cache";
            logger.error(msg, exception);
            throw new RuntimeException(msg, exception);
        }
    }

    public interface Cacheable<T> {
        void cacheTo(DataSaver<T> saver);
        String getFilename();
    }
}
