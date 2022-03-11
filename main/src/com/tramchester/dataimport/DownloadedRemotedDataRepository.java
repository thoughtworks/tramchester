package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LazySingleton
public class DownloadedRemotedDataRepository implements RemoteDataRefreshed {
    private static final Logger logger = LoggerFactory.getLogger(DownloadedRemotedDataRepository.class);

    private final List<DataSourceID> refreshed;
    private final Map<DataSourceID, Path> availableFiles;

    @Inject
    public DownloadedRemotedDataRepository() {
        refreshed = new ArrayList<>();
        availableFiles = new HashMap<>();
    }

    @Override
    public boolean refreshed(DataSourceID dataSourceID) {
        return refreshed.contains(dataSourceID);
    }

    @Override
    public boolean hasFileFor(DataSourceID dataSourceID) {
        return availableFiles.containsKey(dataSourceID);
    }

    @Override
    public Path fileFor(DataSourceID dataSourceID) {
        if (!availableFiles.containsKey(dataSourceID)) {
            final String msg = "No data was downloaded or was available for " + dataSourceID;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return availableFiles.get(dataSourceID);
    }

    public void markRefreshed(DataSourceID dataSourceId) {
        if (refreshed.contains(dataSourceId)) {
            logger.warn(dataSourceId + " already marked as refreshed");
        }
        refreshed.add(dataSourceId);
    }

    public void addFileFor(DataSourceID dataSourceId, Path path) {
        if (availableFiles.containsKey(dataSourceId)) {
            final String message = dataSourceId + " already present in available files";
            logger.error(message);
            throw new RuntimeException(message);
        }
        availableFiles.put(dataSourceId, path);
    }
}
