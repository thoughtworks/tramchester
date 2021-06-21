package com.tramchester.caching;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.RemoteDataRefreshed;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class DataCache {
    private static final Logger logger = LoggerFactory.getLogger(DataCache.class);

    private final Path cacheFolder;
    private final RemoteDataRefreshed remoteDataRefreshed;
    private final CsvMapper mapper;
    private final TramchesterConfig config;
    private boolean ready;

    @Inject
    public DataCache(TramchesterConfig config, RemoteDataRefreshed remoteDataRefreshed, CsvMapper mapper) {
        this.config = config;
        this.cacheFolder = config.getCacheFolder().toAbsolutePath();
        this.remoteDataRefreshed = remoteDataRefreshed;
        this.mapper = mapper;
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

        clearCacheIfDataRefreshed();
    }

    private void clearCacheIfDataRefreshed() {
        Set<String> refreshedSources = config.getRemoteDataSourceConfig().stream().
                map(RemoteDataSourceConfig::getName).
                filter(remoteDataRefreshed::refreshed).collect(Collectors.toSet());
        if (refreshedSources.isEmpty()) {
            return;
        }
        logger.warn("Some data sources (" + refreshedSources+ ") have refreshed, clearning cache " + cacheFolder);
        clearFiles();
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
        if (!Files.exists(cacheFolder)) {
            logger.warn("Not clearing cache, folder not present: " + cacheFolder);
            return;
        }

        logger.warn("Clearing cache");
        try {
            List<Path> files = filesInCache();
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        } catch (IOException exception) {
            final String msg = "Unable to clear cache";
            logger.error(msg, exception);
            throw new RuntimeException(msg, exception);
        }
    }

    public <T> void loadInto(Cacheable<T> cacheable, Class<T> theClass)  {
        if (ready) {
            Path cacheFile = getPathFor(cacheable);
            logger.info("Loading " + cacheFile.toAbsolutePath()  + " to " + theClass.getSimpleName());

            DataLoader<T> loader = new DataLoader<T>(cacheFile, theClass, mapper);
            Stream<T> data = loader.load();
            try {
                cacheable.loadFrom(data);
            } catch (CacheLoadException exception) {
                throw new RuntimeException("Failed to load from cache for "
                        + cacheFile + " and " + theClass.getSimpleName(), exception);
            }
            data.close();
        } else {
            throw new RuntimeException("Attempt to load from " + cacheable.getFilename() + " for " + theClass.getSimpleName()
                    + " when not ready");
        }

    }

    public static class CacheLoadException extends Exception {

        public CacheLoadException(String msg) {
            super(msg);
        }
    }

    public interface Cacheable<T> {
        void cacheTo(DataSaver<T> saver);
        String getFilename();
        void loadFrom(Stream<T> stream) throws CacheLoadException;
    }
}
