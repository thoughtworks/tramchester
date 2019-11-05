package com.tramchester.config;

import java.nio.file.Path;

public interface DownloadConfig {
    // url to load timetable data from
    String getTramDataUrl();
    // url to check mod time against to see if newer data available
    String getTramDataCheckUrl();
    // where to load timetable data from and place preprocessed data
    Path getDataPath();
    // folder tfgm data zip unpacks to
    Path getUnzipPath();

}
