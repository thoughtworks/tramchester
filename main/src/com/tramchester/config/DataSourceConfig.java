package com.tramchester.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Path;

@JsonDeserialize(as=DataSourceAppConfig.class)
public interface DataSourceConfig {
    // url to load timetable data from
    String getTramDataUrl();

    // url to check mod time against to see if newer data available
    String getTramDataCheckUrl();

    // where to load timetable data from and place preprocessed data
    Path getDataPath();

    // folder data zip unpacks to
    Path getUnzipPath();

    // name of fetched zip file
    String getZipFilename();

    String getName();

    boolean getHasFeedInfo();
}
