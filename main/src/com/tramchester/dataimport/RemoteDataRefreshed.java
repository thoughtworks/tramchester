package com.tramchester.dataimport;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.DataSourceID;

import java.nio.file.Path;

@ImplementedBy(DownloadedRemotedDataRepository.class)
public interface RemoteDataRefreshed {
    boolean refreshed(DataSourceID dataSourceID);
    boolean hasFileFor(DataSourceID dataSourceID);
    Path fileFor(DataSourceID dataSourceID);
}
