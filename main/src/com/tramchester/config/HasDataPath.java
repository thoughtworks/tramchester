package com.tramchester.config;

import java.nio.file.Path;

/***
 * Linked between downloaded data, unzipped target and gtfs data source loading
 */
public interface HasDataPath {

    // download destination folder
    Path getDataPath();
}
