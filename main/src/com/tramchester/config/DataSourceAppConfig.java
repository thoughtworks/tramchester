package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

public class DataSourceAppConfig extends Configuration implements DataSourceConfig  {

    @NotNull
    @JsonProperty(value = "name")
    private String name;

    @NotNull
    @JsonProperty("URL")
    private String tramDataUrl;

    @NotNull
    @JsonProperty("checkURL")
    private String tramDataCheckUrl;

    @NotNull
    @JsonProperty("dataPath")
    private Path dataPath;

    @NotNull
    @JsonProperty("unzipPath")
    private Path unzipPath;

    @NotNull
    @JsonProperty("hasFeedInfo")
    private boolean hasFeedInfo;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean getHasFeedInfo() {
        return hasFeedInfo;
    }

    @Override
    public String getTramDataUrl() {
        return tramDataUrl;
    }

    @Override
    public String getTramDataCheckUrl() {
        return tramDataCheckUrl;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public Path getUnzipPath() {
        return unzipPath;
    }

    @Override
    public String getZipFilename() {
        return "data.zip";
    }

    @Override
    public String toString() {
        return "DataSourceAppConfig{" +
                "name='" + name + '\'' +
                ", tramDataUrl='" + tramDataUrl + '\'' +
                ", tramDataCheckUrl='" + tramDataCheckUrl + '\'' +
                ", dataPath=" + dataPath +
                ", unzipPath=" + unzipPath +
                '}';
    }
}
