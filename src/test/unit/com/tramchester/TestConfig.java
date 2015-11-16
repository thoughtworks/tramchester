package com.tramchester;

import com.tramchester.config.TramchesterConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;


public abstract class TestConfig extends TramchesterConfig {
    private List<String> closedStations = asList("St Peters Square");

    @Override
    public boolean isPullData() {
        File file = new File("testData/data.zip");
        return !file.exists();
    }

    @Override
    public List<String> getClosedStations() {
        return closedStations;
    }

    @Override
    public String getInstanceDataBaseURL() {
        return "http://localhost:8080";
    }

    @Override
    public String getTramDataUrl() {
        return "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip";
    }


}
