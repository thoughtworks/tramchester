package com.tramchester.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Path;

@JsonDeserialize(as=RemoteDataSourceAppConfig.class)
public interface RemoteDataSourceConfig {

    // url to check mod time against to see if newer data available
    String getDataCheckUrl();

    // url where data is located
    String getDataUrl();

    // download destination folder
    Path getDataPath();

    // downloaded filename
    String getDownloadFilename();

    // useful name for data set
    String getName();
}
